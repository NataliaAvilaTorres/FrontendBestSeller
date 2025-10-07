package com.example.bestsellerfrontend

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class FormularioNuevaOfertaFragment : Fragment() {

    private lateinit var apiService: ApiService
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
        val etProdNombre = view.findViewById<AutoCompleteTextView>(R.id.etProdNombre)
        val etProdPrecio = view.findViewById<TextInputEditText>(R.id.etProdPrecio)
        val actvTienda = view.findViewById<AutoCompleteTextView>(R.id.actvTienda)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarOferta)
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)

        // --- Cargar tiendas ---
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                tiendas = apiService.listarTiendas()
                if (tiendas.isEmpty()) {
                    Toast.makeText(requireContext(), "No se encontraron tiendas", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val adapterTiendas = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    tiendas.map { it.nombre }
                )
                actvTienda.setAdapter(adapterTiendas)
                actvTienda.setOnClickListener { actvTienda.showDropDown() }

                actvTienda.setOnItemClickListener { _, _, position, _ ->
                    tiendaSeleccionada = tiendas[position]
                    val tiendaId = tiendaSeleccionada?.id
                    if (tiendaId.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "La tienda seleccionada no tiene ID válido", Toast.LENGTH_SHORT).show()
                        return@setOnItemClickListener
                    }
                    Log.d("FormularioNuevaOferta", "Tienda seleccionada: ${tiendaSeleccionada?.nombre}, id=$tiendaId")
                    cargarProductosPorTienda(tiendaId, etProdNombre)
                }

            } catch (e: Exception) {
                Log.e("FormularioNuevaOferta", "Error cargando tiendas: ${e.message}", e)
                Toast.makeText(requireContext(), "Error cargando tiendas", Toast.LENGTH_SHORT).show()
            }
        }

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

        // --- Manejo del producto seleccionado ---
        etProdNombre.setOnItemClickListener { _, _, position, _ ->
            productoSeleccionado = productos[position]
        }
        etProdNombre.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val nombreIngresado = etProdNombre.text.toString()
                productoSeleccionado = productos.find { it.nombre == nombreIngresado }
            }
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
                val mensaje = "Completa los siguientes campos: ${camposFaltantes.joinToString(", ")}"
                Log.d("FormularioNuevaOferta", mensaje)
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaFinalMillis = try {
                dateFormat.parse(fechaFinalStr)?.time ?: cal.timeInMillis
            } catch (_: Exception) {
                cal.timeInMillis
            }

            // --- Crear oferta ---
            val nuevaOferta = Oferta(
                nombreOferta = "Oferta en ${productoSeleccionado!!.nombre}",
                descripcionOferta = descripcion,
                tiendaId = tiendaSeleccionada!!.id,
                fechaOferta = System.currentTimeMillis(),
                fechaFinal = fechaFinalMillis,
                productoId = productoSeleccionado!!.id,
                ubicacion = tiendaSeleccionada!!.ubicacion
            )

            val prefs = requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
            val usuarioId = prefs.getString("id", null)
            if (usuarioId == null) {
                Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- Llamada al backend y actualización de precio ---
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val respuesta = apiService.crearOferta(usuarioId, nuevaOferta)
                    Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()

                    // Actualizar precio del producto hasta la fecha final
                    productoSeleccionado?.id?.let { productoId ->
                        val productoRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("productos")
                            .child(productoId)
                        val updates = mapOf<String, Any>(
                            "precio" to precio!!,
                            "precioHasta" to fechaFinalMillis
                        )
                        productoRef.updateChildren(updates)
                    }

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // --- Botón regresar ---
        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    // --- Cargar productos según tienda ---
    private fun cargarProductosPorTienda(tiendaId: String, etProdNombre: AutoCompleteTextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                productos = apiService.listarProductosTienda(tiendaId)
                val nombresProductos = productos.map { it.nombre }
                Log.d("FormularioNuevaOferta", "Productos cargados: $nombresProductos")

                val adapterProductos = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nombresProductos
                )
                etProdNombre.setAdapter(adapterProductos)
                etProdNombre.showDropDown()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error cargando productos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}