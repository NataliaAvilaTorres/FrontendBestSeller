package com.example.bestsellerfrontend

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class FormularioNuevaOfertaFragment : Fragment() {

    // Servicio API para interactuar con el backend
    private lateinit var apiService: ApiService

    // Elementos de la interfaz
    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador

    // Variables para manejo de fecha
    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Listas y elementos seleccionados
    private var tiendas: List<Tienda> = emptyList()
    private var tiendaSeleccionada: Tienda? = null
    private var productos: List<Producto> = emptyList()
    private var productoSeleccionado: Producto? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.actividad_fromulario_nuevaoferta, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Configuración de Retrofit ---
        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.0.2.2:8090/")
            .baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Referencias a los elementos del layout ---
        val etDescripcion = view.findViewById<TextInputEditText>(R.id.etDescripcion)
        val etFechaFinal = view.findViewById<TextInputEditText>(R.id.etFechaFinal)
        val autoProducto = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerProductos)
        val etProdPrecio = view.findViewById<TextInputEditText>(R.id.etProdPrecio)
        val autoTienda = view.findViewById<MaterialAutoCompleteTextView>(R.id.actvTienda)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarOferta)
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        val etNombreOferta = view.findViewById<TextInputEditText>(R.id.etNombreOferta)

        // --- Cargar tiendas desde la API ---
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                tiendas = apiService.listarTiendas()
                if (tiendas.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontraron tiendas",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Llenamos el autocompletado con los nombres de las tiendas
                val adapterTiendas = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    tiendas.map { it.nombre }
                )

                autoTienda.setAdapter(adapterTiendas)
                autoTienda.setOnClickListener { autoTienda.showDropDown() }

                // Cuando el usuario selecciona una tienda
                autoTienda.setOnItemClickListener { _, _, position, _ ->
                    tiendaSeleccionada = tiendas[position]
                    val tiendaId = tiendaSeleccionada?.id

                    // Validamos el ID de la tienda
                    if (tiendaId.isNullOrEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "La tienda seleccionada no tiene ID válido",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnItemClickListener
                    }

                    Log.d(
                        "FormularioNuevaOferta",
                        "Tienda seleccionada: ${tiendaSeleccionada?.nombre}, id=$tiendaId"
                    )

                    // Cargamos los productos correspondientes a esa tienda
                    cargarProductosPorTienda(tiendaId, autoProducto, etNombreOferta)
                }

            } catch (e: Exception) {
                // Si ocurre un error al obtener tiendas
                Log.e("FormularioNuevaOferta", "Error cargando tiendas: ${e.message}", e)
                Toast.makeText(requireContext(), "Error cargando tiendas", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // --- Configurar RecyclerView de categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Lista de categorías (ícono + nombre)
        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantáneos"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Pastas y Harinas")
        )

        // Adaptador personalizado con clic en cada categoría
        adapterCategorias = CategoriaAdaptador(
            categorias,
            onCategoriaClick = { categoriaSeleccionada ->
                filtrarProductosPorCategoria(categoriaSeleccionada, autoProducto, etNombreOferta)
            },
            clicHabilitado = true
        )
        recyclerViewCategorias.adapter = adapterCategorias

        // --- Selector de fecha final ---
        etFechaFinal.setOnClickListener {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            // Mostramos el DatePicker
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                etFechaFinal.setText(dateFormat.format(cal.time))
            }, y, m, d).show()
        }

        // --- Botón de guardar oferta ---
        btnGuardar.setOnClickListener {
            // Obtenemos valores ingresados
            val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()
            val fechaFinalStr = etFechaFinal.text?.toString()?.trim().orEmpty()
            val precioStr = etProdPrecio.text?.toString()?.trim().orEmpty()
            val precio = precioStr.toDoubleOrNull()

            // Validamos campos requeridos
            val camposFaltantes = mutableListOf<String>()
            if (tiendaSeleccionada == null) camposFaltantes.add("Tienda")
            if (productoSeleccionado == null) camposFaltantes.add("Producto")
            if (descripcion.isEmpty()) camposFaltantes.add("Descripción")
            if (fechaFinalStr.isEmpty()) camposFaltantes.add("Fecha final")
            if (precio == null) camposFaltantes.add("Precio")

            // Si faltan datos, se notifica al usuario
            if (camposFaltantes.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Completa los siguientes campos: ${camposFaltantes.joinToString(", ")}",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Convertimos la fecha final a milisegundos
            val fechaFinalMillis = try {
                dateFormat.parse(fechaFinalStr)?.time ?: cal.timeInMillis
            } catch (_: Exception) {
                cal.timeInMillis
            }

            // Creamos el objeto oferta
            val nuevaOferta = Oferta(
                nombreOferta = etNombreOferta.text?.toString()?.trim().orEmpty(),
                descripcionOferta = descripcion,
                tiendaId = tiendaSeleccionada!!.id,
                fechaOferta = System.currentTimeMillis(),
                fechaFinal = fechaFinalMillis,
                productoId = productoSeleccionado!!.id,
                ubicacion = tiendaSeleccionada!!.ubicacion
            )

            // Obtenemos el ID del usuario logueado
            val prefs = requireContext().getSharedPreferences(
                "usuarioPrefs",
                AppCompatActivity.MODE_PRIVATE
            )
            val usuarioId = prefs.getString("id", null)
            if (usuarioId == null) {
                Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Llamada asíncrona para crear la oferta y actualizar Firebase
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Crear oferta en el backend
                    val respuesta = apiService.crearOferta(usuarioId, nuevaOferta)
                    Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()

                    // Actualizar producto existente en Firebase
                    val tiendaId = tiendaSeleccionada!!.id
                    val productoId = productoSeleccionado!!.id

                    // Validamos que el producto tenga ID
                    if (productoId.isNullOrEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Producto sin ID válido; no se actualizó",
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        return@launch
                    }

                    val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("productos")
                        .child(tiendaId)
                        .child(productoId)

                    // Verificamos si el producto existe antes de modificarlo
                    dbRef.get().addOnSuccessListener { snap ->
                        if (snap.exists()) {
                            val precioFinal: Double = precio!!

                            // ✅ ÚNICO CAMBIO: guarda precio en 'precioHasta' y fecha en 'fechaHasta'
                            val updates = mapOf<String, Any>(
                                "precioHasta" to precioFinal,     // nuevo precio (Double)
                                "fechaHasta" to fechaFinalMillis  // fecha límite (Long)
                            )

                            dbRef.updateChildren(updates).addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "No se pudo actualizar el producto",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // Regresamos a la pantalla anterior
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        } else {
                            // Si no existe, no hacemos nada
                            Toast.makeText(
                                requireContext(),
                                "El producto no existe en la tienda. No se creó ningún nodo.",
                                Toast.LENGTH_LONG
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Error al verificar el producto",
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }

                } catch (e: Exception) {
                    // Si ocurre algún error general
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        // --- Botón regresar ---
        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    // --- Función para cargar productos de una tienda seleccionada ---
    private fun cargarProductosPorTienda(
        tiendaId: String,
        autoProducto: MaterialAutoCompleteTextView,
        etNombreOferta: TextInputEditText
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                productos = apiService.listarProductosTienda(tiendaId)
                actualizarAutoCompleteProductos(productos, autoProducto, etNombreOferta)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error cargando productos", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // --- Filtrar productos por categoría seleccionada ---
    private fun filtrarProductosPorCategoria(
        categoria: String,
        autoProducto: MaterialAutoCompleteTextView,
        etNombreOferta: TextInputEditText
    ) {
        val filtrados = productos.filter {
            it.marca.categoria.trim().equals(categoria.trim(), ignoreCase = true)
        }
        if (filtrados.isEmpty()) {
            Toast.makeText(requireContext(), "No hay productos en '$categoria'", Toast.LENGTH_SHORT)
                .show()
            autoProducto.setAdapter(null)
            return
        }
        actualizarAutoCompleteProductos(filtrados, autoProducto, etNombreOferta)
    }

    // --- Adaptar autocompletado de productos según la lista ---
    private fun actualizarAutoCompleteProductos(
        listaProductos: List<Producto>,
        autoProducto: MaterialAutoCompleteTextView,
        etNombreOferta: TextInputEditText
    ) {
        val nombresProductos = listaProductos.map { it.nombre }
        val adapterProductos = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            nombresProductos
        )

        autoProducto.setAdapter(adapterProductos)
        autoProducto.setOnClickListener { autoProducto.showDropDown() }

        // Cuando se selecciona un producto
        autoProducto.setOnItemClickListener { _, _, position, _ ->
            productoSeleccionado = listaProductos[position]
            etNombreOferta.setText("Oferta en ${productoSeleccionado?.nombre}")
        }
    }
}
