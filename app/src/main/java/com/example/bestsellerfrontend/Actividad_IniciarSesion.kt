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

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        setContentView(R.layout.actividad_iniciar_sesion)

        // Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // backend local en emulador
            //.baseUrl("http://192.168.0.7:8090/") // backend real
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val correoEdit = findViewById<TextInputEditText>(R.id.UsuarioEditarTexto)
        val contrasenaEdit = findViewById<TextInputEditText>(R.id.contrasenaEditarTexto)
        val loginBtn = findViewById<Button>(R.id.iniciarSesionButton)
        val registrateTxt = findViewById<TextView>(R.id.registrate)

        registrateTxt.setOnClickListener {
            val intent = Intent(this, Actividad_Registro::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val correo = correoEdit.text.toString().trim()
            val contrasena = contrasenaEdit.text.toString().trim()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usuario = Usuario("", correo, "", contrasena)

            lifecycleScope.launch {
                try {
                    val respuesta = apiService.login(usuario)
                    Toast.makeText(
                        this@Actividad_IniciarSesion,
                        respuesta.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    if (respuesta.mensaje == "Login exitoso" && respuesta.usuario != null) {

                        val prefs = getSharedPreferences("usuarioPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("id", respuesta.usuario.id)
                        editor.putString("nombre", respuesta.usuario.nombre)
                        editor.putString("correo", respuesta.usuario.correo)
                        editor.putString("ciudad", respuesta.usuario.ciudad)
                        editor.putString("contrasena", respuesta.usuario.contrasena)
                        editor.apply()

                        // Navegar a la pantalla principal
                        val intent = Intent(
                            this@Actividad_IniciarSesion,
                            Actividad_Navegacion_Usuario::class.java
                        )
                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
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