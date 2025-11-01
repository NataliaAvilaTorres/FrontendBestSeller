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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class FormularioNuevaOfertaFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador

    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var tiendas: List<Tienda> = emptyList()
    private var tiendaSeleccionada: Tienda? = null
    private var productos: List<Producto> = emptyList()
    private var productoSeleccionado: Producto? = null

    // Variables para modo edición
    private var modoEdicion = false
    private var ofertaIdEditar: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.actividad_fromulario_nuevaoferta, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Detectar modo: ¿Crear o Editar? ---
        modoEdicion = arguments?.getString("modo") == "editar"
        ofertaIdEditar = arguments?.getString("oferta_id")

        Log.d("FormularioOferta", "Modo: ${if (modoEdicion) "EDITAR" else "CREAR"}")

        // --- Configuración de Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
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

        // ✅ Cambiar título y botón si es edición
        if (modoEdicion) {
            btnGuardar.text = "💾 Guardar Cambios"

            // Pre-llenar campos desde los argumentos
            etNombreOferta.setText(arguments?.getString("oferta_nombre") ?: "")
            etDescripcion.setText(arguments?.getString("oferta_descripcion") ?: "")

            val fechaFinal = arguments?.getLong("oferta_fecha_final", 0L) ?: 0L
            if (fechaFinal > 0) {
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etFechaFinal.setText(formatoFecha.format(Date(fechaFinal)))
            }

            // El precio se cargará desde Firebase cuando se seleccione el producto
        } else {
            btnGuardar.text = "➕ Crear Oferta"
        }

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

                val adapterTiendas = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    tiendas.map { it.nombre }
                )

                autoTienda.setAdapter(adapterTiendas)
                autoTienda.setOnClickListener { autoTienda.showDropDown() }

                // ✅ Si es edición, pre-seleccionar la tienda
                if (modoEdicion) {
                    val tiendaIdEditar = arguments?.getString("oferta_tienda_id")
                    if (!tiendaIdEditar.isNullOrEmpty()) {
                        val tiendaIndex = tiendas.indexOfFirst { it.id == tiendaIdEditar }
                        if (tiendaIndex >= 0) {
                            autoTienda.setText(tiendas[tiendaIndex].nombre, false)
                            tiendaSeleccionada = tiendas[tiendaIndex]
                            cargarProductosPorTienda(tiendaIdEditar, autoProducto, etNombreOferta)
                        }
                    }
                }

                autoTienda.setOnItemClickListener { _, _, position, _ ->
                    tiendaSeleccionada = tiendas[position]
                    val tiendaId = tiendaSeleccionada?.id

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

                    cargarProductosPorTienda(tiendaId, autoProducto, etNombreOferta)
                }

            } catch (e: Exception) {
                Log.e("FormularioNuevaOferta", "Error cargando tiendas: ${e.message}", e)
                Toast.makeText(requireContext(), "Error cargando tiendas", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Configurar RecyclerView de categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantáneos"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Pastas y Harinas")
        )

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
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                etFechaFinal.setText(dateFormat.format(cal.time))
            }, y, m, d).show()
        }

        // --- Botón de guardar oferta ---
        btnGuardar.setOnClickListener {
            val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()
            val fechaFinalStr = etFechaFinal.text?.toString()?.trim().orEmpty()
            val precioStr = etProdPrecio.text?.toString()?.trim().orEmpty()
            val precio = precioStr.toDoubleOrNull()

            // Validaciones
            val camposFaltantes = mutableListOf<String>()
            if (tiendaSeleccionada == null) camposFaltantes.add("Tienda")
            if (productoSeleccionado == null) camposFaltantes.add("Producto")
            if (descripcion.isEmpty()) camposFaltantes.add("Descripción")
            if (fechaFinalStr.isEmpty()) camposFaltantes.add("Fecha final")
            if (precio == null) camposFaltantes.add("Precio")

            if (camposFaltantes.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Completa los siguientes campos: ${camposFaltantes.joinToString(", ")}",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // ✅ Validar que el nuevo precio sea menor al precio original
            if (precio!! >= productoSeleccionado!!.precio) {
                Toast.makeText(
                    requireContext(),
                    "El precio de la oferta debe ser menor a $${productoSeleccionado!!.precio}",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val fechaFinalMillis = try {
                dateFormat.parse(fechaFinalStr)?.time ?: cal.timeInMillis
            } catch (_: Exception) {
                cal.timeInMillis
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (modoEdicion && ofertaIdEditar != null) {
                        // ✅ MODO EDITAR
                        Log.d("FormularioOferta", "Editando oferta: $ofertaIdEditar")

                        val ofertaActualizada = Oferta(
                            id = ofertaIdEditar!!,
                            nombreOferta = etNombreOferta.text?.toString()?.trim().orEmpty(),
                            descripcionOferta = descripcion,
                            tiendaId = tiendaSeleccionada!!.id,
                            fechaOferta = System.currentTimeMillis(),
                            fechaFinal = fechaFinalMillis,
                            productoId = productoSeleccionado!!.id,
                            ubicacion = tiendaSeleccionada!!.ubicacion
                        )

                        // ❌ No llamar al backend para evitar que elimine la oferta
                        Log.d("FormularioOferta", "Actualizando solo Firebase")

                        // Actualizar precioHasta en Firebase
                        val tiendaId = tiendaSeleccionada!!.id
                        val productoId = productoSeleccionado!!.id

                        if (tiendaId.isNullOrEmpty() || productoId.isNullOrEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "Producto o tienda sin ID válido",
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                            return@launch
                        }

                        val dbRef = FirebaseDatabase.getInstance()
                            .getReference("productos")
                            .child(tiendaId)
                            .child(productoId)

                        Log.d("Firebase", "Editando producto: productos/$tiendaId/$productoId")

                        val updatesEditar = mapOf<String, Any>(
                            "precioHasta" to precio!!,
                            "fechaHasta" to fechaFinalMillis
                        )

                        dbRef.updateChildren(updatesEditar)
                            .addOnSuccessListener {
                                Log.d("Firebase", "✅ Oferta editada en Firebase")
                                Toast.makeText(
                                    requireContext(),
                                    "✅ Oferta actualizada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase", "❌ Error: ${exception.message}", exception)
                                Toast.makeText(
                                    requireContext(),
                                    "Error al actualizar: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }

                    } else {
                        // ✅ MODO CREAR (código original)
                        val nuevaOferta = Oferta(
                            nombreOferta = etNombreOferta.text?.toString()?.trim().orEmpty(),
                            descripcionOferta = descripcion,
                            tiendaId = tiendaSeleccionada!!.id,
                            fechaOferta = System.currentTimeMillis(),
                            fechaFinal = fechaFinalMillis,
                            productoId = productoSeleccionado!!.id,
                            ubicacion = tiendaSeleccionada!!.ubicacion
                        )

                        val prefs = requireContext().getSharedPreferences(
                            "usuarioPrefs",
                            AppCompatActivity.MODE_PRIVATE
                        )
                        val usuarioId = prefs.getString("id", null)
                        if (usuarioId == null) {
                            Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val respuesta = apiService.crearOferta(usuarioId, nuevaOferta)
                        Log.d("Firebase", "Oferta creada: ${respuesta.mensaje}")
                        Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()

                        val tiendaId = tiendaSeleccionada!!.id
                        val productoId = productoSeleccionado!!.id

                        if (productoId.isNullOrEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "Producto sin ID válido",
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                            return@launch
                        }

                        val dbRef = FirebaseDatabase.getInstance()
                            .getReference("productos")
                            .child(tiendaId)
                            .child(productoId)

                        Log.d("Firebase", "Ruta: productos/$tiendaId/$productoId")

                        val updates = mapOf<String, Any>(
                            "precioHasta" to precio!!,
                            "fechaHasta" to fechaFinalMillis
                        )

                        dbRef.updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d("Firebase", "✅ Actualización exitosa")
                                Toast.makeText(
                                    requireContext(),
                                    "✅ Oferta creada y producto actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase", "❌ Error: ${exception.message}", exception)
                                Toast.makeText(
                                    requireContext(),
                                    "Error al actualizar Firebase: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                    }

                } catch (e: Exception) {
                    Log.e("Firebase", "❌ Excepción: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun cargarProductosPorTienda(
        tiendaId: String?,
        autoProducto: MaterialAutoCompleteTextView,
        etNombreOferta: TextInputEditText
    ) {
        if (tiendaId.isNullOrEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                productos = apiService.listarProductosTienda(tiendaId)
                actualizarAutoCompleteProductos(productos, autoProducto, etNombreOferta)

                // ✅ Si es edición, pre-seleccionar el producto
                if (modoEdicion) {
                    val productoIdEditar = arguments?.getString("oferta_producto_id")
                    if (!productoIdEditar.isNullOrEmpty()) {
                        val productoSeleccionadoObj = productos.find { it.id == productoIdEditar }
                        if (productoSeleccionadoObj != null) {
                            productoSeleccionado = productoSeleccionadoObj
                            autoProducto.setText(productoSeleccionadoObj.nombre, false)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error cargando productos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filtrarProductosPorCategoria(
        categoria: String,
        autoProducto: MaterialAutoCompleteTextView,
        etNombreOferta: TextInputEditText
    ) {
        val filtrados = productos.filter {
            it.marca.categoria.trim().equals(categoria.trim(), ignoreCase = true)
        }
        if (filtrados.isEmpty()) {
            Toast.makeText(requireContext(), "No hay productos en '$categoria'", Toast.LENGTH_SHORT).show()
            autoProducto.setAdapter(null)
            return
        }
        actualizarAutoCompleteProductos(filtrados, autoProducto, etNombreOferta)
    }

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

        autoProducto.setOnItemClickListener { _, _, position, _ ->
            productoSeleccionado = listaProductos[position]
            etNombreOferta.setText("Oferta en ${productoSeleccionado?.nombre}")
        }
    }
}