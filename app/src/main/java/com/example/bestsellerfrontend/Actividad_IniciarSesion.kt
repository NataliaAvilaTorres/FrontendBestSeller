package com.example.bestsellerfrontend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Actividad_IniciarSesion : AppCompatActivity() {

    // Servicio para llamadas a la API (Retrofit)
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase solo si aún no está inicializado
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        // Vincular el layout XML de esta actividad
        setContentView(R.layout.actividad_iniciar_sesion)

        // --- Configuración de Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.16:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crear una instancia del servicio API a partir de Retrofit
        apiService = retrofit.create(ApiService::class.java)

        // --- Referencias a los elementos de la vista ---
        val correoEdit = findViewById<TextInputEditText>(R.id.UsuarioEditarTexto)
        val contrasenaEdit = findViewById<TextInputEditText>(R.id.contrasenaEditarTexto)
        val loginBtn = findViewById<Button>(R.id.iniciarSesionButton)
        val registrateTxt = findViewById<TextView>(R.id.registrate)

        // --- Redirección al registro ---
        registrateTxt.setOnClickListener {
            val intent = Intent(this, Actividad_Registro::class.java)
            startActivity(intent)
        }

        // --- Evento al presionar "Iniciar sesión" ---
        loginBtn.setOnClickListener {
            // Obtener texto ingresado por el usuario
            val correo = correoEdit.text.toString().trim()
            val contrasena = contrasenaEdit.text.toString().trim()

            // Validar que los campos no estén vacíos
            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear objeto Usuario para enviar al backend
            val usuario = Usuario("", correo, "", contrasena)

            // --- Llamada asíncrona al backend con corrutinas ---
            lifecycleScope.launch {
                try {
                    // Enviar solicitud de login al backend
                    val respuesta = apiService.login(usuario)

                    // Mostrar mensaje recibido desde el servidor (éxito o error)
                    Toast.makeText(
                        this@Actividad_IniciarSesion,
                        respuesta.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Si el login fue exitoso y se devolvió el usuario:
                    if (respuesta.mensaje == "Login exitoso" && respuesta.usuario != null) {

                        // --- Guardar datos del usuario en SharedPreferences ---
                        val prefs = getSharedPreferences("usuarioPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("id", respuesta.usuario.id)
                        editor.putString("nombre", respuesta.usuario.nombre)
                        editor.putString("correo", respuesta.usuario.correo)
                        editor.putString("ciudad", respuesta.usuario.ciudad)
                        editor.putString("contrasena", respuesta.usuario.contrasena)
                        editor.apply() // Guarda los cambios de manera asíncrona

                        // --- Navegar a la actividad principal del usuario ---
                        val intent = Intent(this@Actividad_IniciarSesion, Actividad_Navegacion_Usuario::class.java)
                        startActivity(intent)
                        finish() // Cierra esta actividad para que no vuelva con "back"
                    }

                } catch (e: Exception) {
                    // Captura cualquier error de conexión o excepción inesperada
                    Toast.makeText(
                        this@Actividad_IniciarSesion,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}