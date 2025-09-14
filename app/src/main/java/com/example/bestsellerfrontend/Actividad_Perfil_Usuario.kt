package com.example.bestsellerfrontend

import android.os.Bundle
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

class Actividad_Perfil_Usuario : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_perfil_usuario, container, false)

        prefs = requireActivity().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)

        val id = prefs.getString("id", "") ?: ""
        val nombre = prefs.getString("nombre", "Usuario") ?: ""
        val correo = prefs.getString("correo", "Sin correo") ?: ""
        val ciudad = prefs.getString("ciudad", "Ciudad") ?: ""
        val contrasena = prefs.getString("contrasena", "Contrasena") ?: ""

        val nombreTextView = view.findViewById<TextView>(R.id.tvNombre)
        val correoTextView = view.findViewById<TextView>(R.id.tvCorreo)
        val ciudadTextView = view.findViewById<TextView>(R.id.ciudad)
        val contrasenaTextView = view.findViewById<TextView>(R.id.tvContrasena)
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        val btnPublicaciones = view.findViewById<ImageView>(R.id.btnPublicaciones)

        nombreTextView.text = nombre
        correoTextView.text = correo
        ciudadTextView.text = ciudad
        contrasenaTextView.text = "*".repeat(contrasena.length)

        // Inicializar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Botón regresar
        btnRegresar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        btnPublicaciones.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Ver_Publicaciones())
                .addToBackStack(null)
                .commit()
        }

        // Listeners de editar
        view.findViewById<ImageView>(R.id.btnEditarNombre).setOnClickListener {
            mostrarDialogoEditar("Editar Nombre", nombreTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "nombre", nombreTextView)
            }
        }

        view.findViewById<ImageView>(R.id.btnEditarCorreo).setOnClickListener {
            mostrarDialogoEditar("Editar Correo", correoTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "correo", correoTextView)
            }
        }

        view.findViewById<ImageView>(R.id.btnEditarCiudad).setOnClickListener {
            mostrarDialogoEditar("Editar Ciudad", ciudadTextView.text.toString()) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "ciudad", ciudadTextView)
            }
        }

        view.findViewById<ImageView>(R.id.btnEditarContrasena).setOnClickListener {
            mostrarDialogoEditar("Editar Contraseña", contrasena) { nuevoValor ->
                actualizarCampoUsuario(id, nuevoValor, "contrasena", contrasenaTextView)
            }
        }

        return view
    }

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

    private fun actualizarCampoUsuario(id: String, nuevoValor: String, campo: String, textView: TextView) {
        val usuario = Usuario(
            nombre = if (campo == "nombre") nuevoValor else prefs.getString("nombre", "") ?: "",
            correo = if (campo == "correo") nuevoValor else prefs.getString("correo", "") ?: "",
            ciudad = if (campo == "ciudad") nuevoValor else prefs.getString("ciudad", "") ?: "",
            contrasena = if (campo == "contrasena") nuevoValor else prefs.getString("contrasena", "") ?: "",
            id = id
        )

        lifecycleScope.launch {
            try {
                val respuesta = apiService.actualizarUsuario(id, usuario)
                if (respuesta.usuario != null) {
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
}