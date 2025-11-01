package com.example.bestsellerfrontend

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.android.material.button.MaterialButton
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide

class Actividad_Perfil_Usuario : Fragment() {

    // Variables globales
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private val storageRef = FirebaseStorage.getInstance().reference
    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null
    private lateinit var fotoPerfil: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_perfil_usuario, container, false)

        // Obtiene las preferencias del usuario (datos guardados tras iniciar sesión)
        prefs = requireActivity().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)

        // Carga los datos guardados del usuario
        val id = prefs.getString("id", "") ?: ""
        val nombre = prefs.getString("nombre", "Usuario") ?: ""
        val correo = prefs.getString("correo", "Sin correo") ?: ""
        val ciudad = prefs.getString("ciudad", "Ciudad") ?: ""
        val contrasena = prefs.getString("contrasena", "Contrasena") ?: ""
        val urlImagen = prefs.getString("urlImagen", null)

        // Referencias a los elementos del layout
        val nombreTextView = view.findViewById<TextView>(R.id.tvNombre)
        val correoTextView = view.findViewById<TextView>(R.id.tvCorreo)
        val ciudadTextView = view.findViewById<TextView>(R.id.ciudad)
        val contrasenaTextView = view.findViewById<TextView>(R.id.tvContrasena)
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        val btnPublicaciones = view.findViewById<ImageView>(R.id.btnPublicaciones)
        val btnEliminarCuenta = view.findViewById<MaterialButton>(R.id.btnEliminarCuenta)
        val btnCerrarSesion = view.findViewById<MaterialButton>(R.id.btnCerrarSesion)
        val btnEditarFoto = view.findViewById<MaterialButton>(R.id.btnEditarFoto)
        fotoPerfil = view.findViewById(R.id.btnVerFoto)

        // Muestra los datos del usuario en los TextView
        nombreTextView.text = nombre
        correoTextView.text = correo
        ciudadTextView.text = ciudad
        contrasenaTextView.text = "*".repeat(contrasena.length) // Oculta la contraseña con asteriscos

        // Carga la foto de perfil si existe una URL guardada
        if (!urlImagen.isNullOrEmpty()) {
            Glide.with(this).load(urlImagen).into(fotoPerfil)
        }

        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.0.2.2:8090/")
            .baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Botón "Regresar" → vuelve al fragmento anterior
        btnRegresar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Botón "Publicaciones" → muestra las publicaciones del usuario
        btnPublicaciones.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Ver_Publicaciones())
                .addToBackStack(null)
                .commit()
        }

        // Botón "Cerrar Sesión" → borra los datos y redirige al login
        btnCerrarSesion.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(requireContext(), "Sesión cerrada con éxito", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), Actividad_IniciarSesion::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Botón "Eliminar Cuenta" → muestra un diálogo de confirmación
        btnEliminarCuenta.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    eliminarCuenta(id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Botón "Editar Foto" → permite seleccionar una imagen del dispositivo
        btnEditarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Editar nombre
        view.findViewById<ImageView>(R.id.btnEditarNombre).setOnClickListener {
            mostrarDialogoEditar("Editar Nombre", nombreTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "nombre", nombreTextView)
            }
        }

        // Editar correo
        view.findViewById<ImageView>(R.id.btnEditarCorreo).setOnClickListener {
            mostrarDialogoEditar("Editar Correo", correoTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "correo", correoTextView)
            }
        }

        // Editar ciudad
        view.findViewById<ImageView>(R.id.btnEditarCiudad).setOnClickListener {
            mostrarDialogoEditar("Editar Ciudad", ciudadTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "ciudad", ciudadTextView)
            }
        }

        // Editar contraseña
        view.findViewById<ImageView>(R.id.btnEditarContrasena).setOnClickListener {
            mostrarDialogoEditar("Editar Contraseña", contrasena) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "contrasena", contrasenaTextView)
            }
        }

        return view
    }

    // Metodo que recibe el resultado al seleccionar una imagen del dispositivo
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            if (imageUri != null) {
                subirImagenAFirebase(imageUri!!)
            }
        }
    }

    // Sube la imagen seleccionada al almacenamiento de Firebase y actualiza la base de datos
    private fun subirImagenAFirebase(uri: Uri) {
        val id = prefs.getString("id", "") ?: return
        val fileRef = storageRef.child("usuarios/$id/perfil.jpg")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                // Obtiene la URL de descarga de la nueva imagen
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val nuevaUrl = downloadUri.toString()

                    // Crea el objeto usuario actualizado
                    val usuario = Usuario(
                        nombre = prefs.getString("nombre", "") ?: "",
                        correo = prefs.getString("correo", "") ?: "",
                        ciudad = prefs.getString("ciudad", "") ?: "",
                        contrasena = prefs.getString("contrasena", "") ?: "",
                        urlImagen = nuevaUrl,
                        id = id
                    )

                    // Actualiza la información del usuario en el servidor
                    lifecycleScope.launch {
                        try {
                            val respuesta = apiService.actualizarUsuario(id, usuario)
                            if (respuesta.usuario != null) {
                                prefs.edit().putString("urlImagen", nuevaUrl).apply()
                                Glide.with(this@Actividad_Perfil_Usuario)
                                    .load(nuevaUrl)
                                    .into(fotoPerfil)
                                Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    // Muestra un cuadro de diálogo para editar un campo del perfil
    private fun mostrarDialogoEditar(titulo: String, valorActual: String, callback: (String) -> Unit) {
        val editText = EditText(requireContext())
        editText.setText(valorActual)

        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoValor = editText.text.toString().trim()
                if (nuevoValor.isNotEmpty()) callback(nuevoValor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Actualiza un campo específico del usuario (nombre, correo, ciudad o contraseña)
    private fun actualizarCampoUsuario(id: String, nuevoValor: String, campo: String, textView: TextView) {
        val usuario = Usuario(
            nombre = if (campo == "nombre") nuevoValor else prefs.getString("nombre", "") ?: "",
            correo = if (campo == "correo") nuevoValor else prefs.getString("correo", "") ?: "",
            ciudad = if (campo == "ciudad") nuevoValor else prefs.getString("ciudad", "") ?: "",
            contrasena = if (campo == "contrasena") nuevoValor else prefs.getString("contrasena", "") ?: "",
            urlImagen = prefs.getString("urlImagen", null),
            id = id
        )

        // Llama al servicio API para guardar los cambios
        lifecycleScope.launch {
            try {
                val respuesta = apiService.actualizarUsuario(id, usuario)
                if (respuesta.usuario != null) {
                    // Actualiza el campo en la vista
                    textView.text = if (campo == "contrasena") "*".repeat(nuevoValor.length) else nuevoValor
                    val editor = prefs.edit()
                    editor.putString(campo, nuevoValor)
                    editor.apply()
                    Toast.makeText(requireContext(), "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${respuesta.mensaje}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Elimina la cuenta del usuario y limpia los datos locales
    private fun eliminarCuenta(id: String) {
        lifecycleScope.launch {
            try {
                val respuesta = apiService.eliminarUsuario(id)
                if (respuesta.mensaje == "Usuario eliminado correctamente") {
                    prefs.edit().clear().apply()
                    Toast.makeText(requireContext(), "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), Actividad_IniciarSesion::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Error: ${respuesta.mensaje}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}