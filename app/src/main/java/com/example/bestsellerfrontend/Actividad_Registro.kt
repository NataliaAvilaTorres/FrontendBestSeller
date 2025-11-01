package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import android.widget.ImageView
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Intent

class Actividad_Registro : AppCompatActivity() {

    // Servicio API para interactuar con el backend
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.actividad_registro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnAtras = findViewById<ImageView>(R.id.btn_atras)
        btnAtras.setOnClickListener {
            finish() // Cierra la actividad actual
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Inicializa el servicio API con la interfaz definida
        apiService = retrofit.create(ApiService::class.java)

        // --- CONFIGURACIÓN DEL SPINNER (Lista desplegable de ciudades) ---
        val ciudadSpinner: Spinner = findViewById(R.id.ciudadSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ciudades_array, // Lista de ciudades definida en strings.xml
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ciudadSpinner.adapter = adapter
        ciudadSpinner.setSelection(0) // Selecciona la primera opción por defecto

        // --- ENLACE DE CAMPOS DEL FORMULARIO ---
        val nombreEditarTexto = findViewById<TextInputEditText>(R.id.nombreEditarTexto)
        val correoEditarTexto = findViewById<TextInputEditText>(R.id.correoEditarTexto)
        val contrasenaEditarTexto = findViewById<TextInputEditText>(R.id.contrasenaEditarTexto)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // --- EVENTO AL PRESIONAR EL BOTÓN DE REGISTRO ---
        registerButton.setOnClickListener {
            // Obtiene los valores ingresados por el usuario
            val nombre = nombreEditarTexto.text.toString().trim()
            val correo = correoEditarTexto.text.toString().trim()
            val ciudad = ciudadSpinner.selectedItem.toString()
            val contrasena = contrasenaEditarTexto.text.toString().trim()

            // Validación de campos vacíos
            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Crea un objeto Usuario con los datos ingresados
            val usuario = Usuario(nombre, correo, ciudad, contrasena)
            registerButton.isEnabled = false // Desactiva el botón mientras se procesa la solicitud

            // --- LLAMADA ASÍNCRONA AL SERVIDOR CON COROUTINES ---
            lifecycleScope.launch {
                try {
                    // Envía los datos al backend usando Retrofit
                    val respuesta = apiService.registrarUsuario(usuario)

                    // Muestra mensaje del servidor (éxito o error)
                    Toast.makeText(this@Actividad_Registro, respuesta.mensaje, Toast.LENGTH_SHORT)
                        .show()

                    // Si el registro fue exitoso, guarda los datos del usuario en SharedPreferences
                    if (respuesta.usuario != null) {
                        val prefs = getSharedPreferences("usuarioPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("id", respuesta.usuario.id ?: "")
                        editor.putString("nombre", respuesta.usuario.nombre)
                        editor.putString("correo", respuesta.usuario.correo)
                        editor.putString("ciudad", respuesta.usuario.ciudad)
                        editor.putString("contrasena", respuesta.usuario.contrasena)
                        editor.apply()

                        // Redirige al usuario a la pantalla principal
                        val intent = Intent(
                            this@Actividad_Registro,
                            Actividad_Navegacion_Usuario::class.java
                        )
                        intent.putExtra("usuarioNombre", respuesta.usuario.nombre)
                        startActivity(intent)
                        finish() // Finaliza la actividad actual
                    }

                    // Limpia los campos del formulario después del registro
                    nombreEditarTexto.text?.clear()
                    correoEditarTexto.text?.clear()
                    contrasenaEditarTexto.text?.clear()
                    ciudadSpinner.setSelection(0)

                } catch (e: Exception) {
                    // Manejo de errores de red o servidor
                    Toast.makeText(
                        this@Actividad_Registro,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    // Reactiva el botón de registro
                    registerButton.isEnabled = true
                }
            }
        }
    }
}