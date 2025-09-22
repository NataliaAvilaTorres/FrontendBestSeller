package com.example.bestsellerfrontend

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormularioNuevaOfertaFragment : Fragment() {

    private lateinit var apiService: ApiService
    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.actividad_fromulario_nuevaoferta, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // cambia si usas red local
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val etNombreOferta = view.findViewById<TextInputEditText>(R.id.etNombreOferta)
        val etDescripcion = view.findViewById<TextInputEditText>(R.id.etDescripcion)
        val etTienda = view.findViewById<TextInputEditText>(R.id.etTienda)
        val etFecha = view.findViewById<TextInputEditText>(R.id.etFecha)
        val etUrlImagenOferta = view.findViewById<TextInputEditText>(R.id.etUrlImagenOferta)

        val etProdNombre = view.findViewById<TextInputEditText>(R.id.etProdNombre)
        val etProdMarca = view.findViewById<TextInputEditText>(R.id.etProdMarca)
        val etProdCategoria = view.findViewById<TextInputEditText>(R.id.etProdCategoria)
        val etProdPrecio = view.findViewById<TextInputEditText>(R.id.etProdPrecio)
        val etProdUrlImagen = view.findViewById<TextInputEditText>(R.id.etProdUrlImagen)

        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarOferta)

        // 📅 Selector de fecha
        etFecha.setOnClickListener {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                etFecha.setText(dateFormat.format(cal.time))
            }, y, m, d).show()
        }

        // ✅ Guardar oferta
        btnGuardar.setOnClickListener {
            val nombreOferta = etNombreOferta.text?.toString()?.trim().orEmpty()
            val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()
            val tienda = etTienda.text?.toString()?.trim().orEmpty()
            val fechaStr = etFecha.text?.toString()?.trim().orEmpty()
            val urlImgOferta = etUrlImagenOferta.text?.toString()?.trim().orEmpty()

            val prodNombre = etProdNombre.text?.toString()?.trim().orEmpty()
            val prodMarca = etProdMarca.text?.toString()?.trim().orEmpty()
            val prodCategoria = etProdCategoria.text?.toString()?.trim().orEmpty()
            val prodPrecioStr = etProdPrecio.text?.toString()?.trim().orEmpty()
            val prodUrlImagen = etProdUrlImagen.text?.toString()?.trim().orEmpty()

            if (nombreOferta.isEmpty() || descripcion.isEmpty() || tienda.isEmpty() ||
                fechaStr.isEmpty() || urlImgOferta.isEmpty() ||
                prodNombre.isEmpty() || prodMarca.isEmpty() || prodCategoria.isEmpty() ||
                prodPrecioStr.isEmpty() || prodUrlImagen.isEmpty()
            ) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val precio = prodPrecioStr.toDoubleOrNull()
            if (precio == null) {
                Toast.makeText(requireContext(), "Precio inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaMillis = try {
                dateFormat.parse(fechaStr)?.time ?: cal.timeInMillis
            } catch (_: Exception) {
                cal.timeInMillis
            }

            val producto = Producto(
                categoria = prodCategoria,
                marca = prodMarca,
                nombre = prodNombre,
                precio = precio,
                urlImagen = prodUrlImagen
            )

            val oferta = Oferta(
                nombreOferta = nombreOferta,
                descripcionOferta = descripcion,
                fechaOferta = fechaMillis,
                tiendaNombre = tienda,
                urlImagen = urlImgOferta,
                producto = producto
            )

            btnGuardar.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // 1️⃣ Guardar en backend
                    val respuesta = apiService.crearOferta(oferta)
                    Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()

                    // 2️⃣ Guardar en Firebase
                    val database = FirebaseDatabase.getInstance()
                    val ofertasRef = database.getReference("ofertas")
                    val notificacionesRef = database.getReference("notificaciones")

                    val idOferta = ofertasRef.push().key!! // ID único
                    ofertasRef.child(idOferta).setValue(oferta)

                    val idNotificacion = notificacionesRef.push().key!! // ID único
                    val notificacion = mapOf(
                        "id" to idNotificacion,
                        "idOferta" to idOferta,
                        "usuario" to tienda, // 👈 aquí puedes usar el usuario logueado
                        "mensaje" to nombreOferta,
                        "timestamp" to System.currentTimeMillis()
                    )
                    notificacionesRef.child(idNotificacion).setValue(notificacion)

                    // Volver atrás
                    requireActivity().onBackPressedDispatcher.onBackPressed()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    btnGuardar.isEnabled = true
                }
            }
        }
    }
}
