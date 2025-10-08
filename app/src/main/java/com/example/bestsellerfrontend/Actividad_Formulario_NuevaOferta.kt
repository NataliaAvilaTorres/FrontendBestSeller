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

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador
    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val etDescripcion = view.findViewById<TextInputEditText>(R.id.etDescripcion)
        val etFechaFinal = view.findViewById<TextInputEditText>(R.id.etFechaFinal)
        val autoProducto = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerProductos)
        val etProdPrecio = view.findViewById<TextInputEditText>(R.id.etProdPrecio)
        val autoTienda = view.findViewById<MaterialAutoCompleteTextView>(R.id.actvTienda)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarOferta)
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        val etNombreOferta = view.findViewById<TextInputEditText>(R.id.etNombreOferta)

        // --- Cargar tiendas ---
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
                Toast.makeText(requireContext(), "Error cargando tiendas", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // --- Categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos y Cereales"),
            Pair(R.drawable.precodidos, "Precocidos"),
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

        // --- Fecha final ---
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

        // --- Guardar oferta ---
        btnGuardar.setOnClickListener {
            val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()
            val fechaFinalStr = etFechaFinal.text?.toString()?.trim().orEmpty()
            val precioStr = etProdPrecio.text?.toString()?.trim().orEmpty()
            val precio = precioStr.toDoubleOrNull()

            val camposFaltantes = mutableListOf<String>()
            if (tiendaSeleccionada == null) camposFaltantes.add("Tienda")
            if (productoSeleccionado == null) camposFaltantes.add("Producto")
            if (descripcion.isEmpty()) camposFaltantes.add("Descripción")
            if (fechaFinalStr.isEmpty()) camposFaltantes.add("Fecha final")
            if (precio == null) camposFaltantes.add("Precio")

            if (camposFaltantes.isNotEmpty()) {
                val mensaje =
                    "Completa los siguientes campos: ${camposFaltantes.joinToString(", ")}"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaFinalMillis = try {
                dateFormat.parse(fechaFinalStr)?.time ?: cal.timeInMillis
            } catch (_: Exception) {
                cal.timeInMillis
            }

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
                Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val respuesta = apiService.crearOferta(usuarioId, nuevaOferta)
                    Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()

                    productoSeleccionado?.id?.let { productoId ->
                        val productoRef =
                            com.google.firebase.database.FirebaseDatabase.getInstance()
                                .getReference("productos")
                                .child(productoId)
                        val updates = mapOf<String, Any>(
                            "precio" to precio!!,
                            "precioHasta" to fechaFinalMillis
                        )
                        productoRef.updateChildren(updates)
                    }

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    // --- Cargar productos ---
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

    // --- Filtrar productos por categoría ---
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

    // --- Adaptador para productos con autocompletado ---
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