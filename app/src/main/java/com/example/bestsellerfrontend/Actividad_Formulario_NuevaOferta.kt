package com.example.bestsellerfrontend

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class FormularioNuevaOfertaFragment : Fragment() {

    private lateinit var apiService: ApiService
    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var imgPreview: ImageView
    private var imagenUri: Uri? = null
    private lateinit var storage: FirebaseStorage

    private val GALLERY_REQUEST = 1001
    private val CAMERA_REQUEST = 1002
    private val CAMERA_PERMISSION_REQUEST = 2001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.actividad_fromulario_nuevaoferta, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = Firebase.storage

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.7:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val etNombreOferta = view.findViewById<TextInputEditText>(R.id.etNombreOferta)
        val etDescripcion = view.findViewById<TextInputEditText>(R.id.etDescripcion)
        val etTienda = view.findViewById<TextInputEditText>(R.id.etTienda)
        val etFecha = view.findViewById<TextInputEditText>(R.id.etFecha)

        val etProdNombre = view.findViewById<TextInputEditText>(R.id.etProdNombre)
        val etProdMarca = view.findViewById<TextInputEditText>(R.id.etProdMarca)
        val etProdCategoria = view.findViewById<TextInputEditText>(R.id.etProdCategoria)
        val etProdPrecio = view.findViewById<TextInputEditText>(R.id.etProdPrecio)

        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarOferta)
        val btnGaleria = view.findViewById<Button>(R.id.btnSeleccionarImagen)
        val btnCamara = view.findViewById<Button>(R.id.btnTomarFoto)
        imgPreview = view.findViewById(R.id.imgPreviewOferta)

        // Selector de fecha
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

        // Galería
        btnGaleria.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST)
        }

        // Cámara con verificación de permiso
        btnCamara.setOnClickListener {
            verificarPermisoCamara()
        }

        // Guardar
        btnGuardar.setOnClickListener {
            val nombreOferta = etNombreOferta.text?.toString()?.trim().orEmpty()
            val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()
            val tienda = etTienda.text?.toString()?.trim().orEmpty()
            val fechaStr = etFecha.text?.toString()?.trim().orEmpty()

            val prodNombre = etProdNombre.text?.toString()?.trim().orEmpty()
            val prodMarca = etProdMarca.text?.toString()?.trim().orEmpty()
            val prodCategoria = etProdCategoria.text?.toString()?.trim().orEmpty()
            val prodPrecioStr = etProdPrecio.text?.toString()?.trim().orEmpty()

            if (nombreOferta.isEmpty() || descripcion.isEmpty() || tienda.isEmpty() ||
                fechaStr.isEmpty() || imagenUri == null ||
                prodNombre.isEmpty() || prodMarca.isEmpty() || prodCategoria.isEmpty() ||
                prodPrecioStr.isEmpty()
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

            val ref = storage.reference.child("ofertas/${UUID.randomUUID()}.jpg")
            val uploadTask = ref.putFile(imagenUri!!)

            uploadTask.addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->

                    val producto = Producto(
                        categoria = prodCategoria,
                        marca = prodMarca,
                        nombre = prodNombre,
                        precio = precio,
                        urlImagen = downloadUri.toString()
                    )

                    val nuevaOferta = Oferta(
                        nombreOferta = nombreOferta,
                        descripcionOferta = descripcion,
                        tiendaNombre = tienda,
                        fechaOferta = fechaMillis,
                        producto = producto
                    )

                    val prefs = requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
                    val usuarioId = prefs.getString("id", null)

                    if (usuarioId == null) {
                        Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val respuesta = apiService.crearOferta(usuarioId, nuevaOferta)
                            Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 📸 Verificación de permisos
    private fun verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            abrirCamara()
        }
    }

    // 📸 Abrir cámara
    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST)
    }

    // Callback permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamara()
        } else if (requestCode == CAMERA_PERMISSION_REQUEST) {
            Toast.makeText(requireContext(), "Se necesita el permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Recibir imagen seleccionada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST && data != null) {
                imagenUri = data.data
                imgPreview.setImageURI(imagenUri)
            } else if (requestCode == CAMERA_REQUEST && data?.extras != null) {
                val bitmap = data.extras!!.get("data") as android.graphics.Bitmap
                imgPreview.setImageBitmap(bitmap)

                val path = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    bitmap,
                    "temp_${System.currentTimeMillis()}",
                    null
                )
                imagenUri = Uri.parse(path)
            }
        }
    }
}