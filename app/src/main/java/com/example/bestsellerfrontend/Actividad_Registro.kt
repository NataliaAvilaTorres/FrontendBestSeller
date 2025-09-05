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
            finish()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Spinner de ciudades
        val ciudadSpinner: Spinner = findViewById(R.id.ciudadSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ciudades_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ciudadSpinner.adapter = adapter
        ciudadSpinner.setSelection(0)

        val nombreEditarTexto = findViewById<TextInputEditText>(R.id.nombreEditarTexto)
        val correoEditarTexto = findViewById<TextInputEditText>(R.id.correoEditarTexto)
        val contrasenaEditarTexto = findViewById<TextInputEditText>(R.id.contrasenaEditarTexto)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val nombre = nombreEditarTexto.text.toString().trim()
            val correo = correoEditarTexto.text.toString().trim()
            val ciudad = ciudadSpinner.selectedItem.toString()
            val contrasena = contrasenaEditarTexto.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usuario = Usuario(nombre, correo, ciudad, contrasena)
            registerButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    apiService.registrarUsuario(usuario)
                    Toast.makeText(this@Actividad_Registro, "Usuario guardado", Toast.LENGTH_SHORT).show()

                    nombreEditarTexto.text?.clear()
                    correoEditarTexto.text?.clear()
                    contrasenaEditarTexto.text?.clear()
                    ciudadSpinner.setSelection(0)

                    val intent = Intent(this@Actividad_Registro, Actividad_Navegacion_Usuario::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@Actividad_Registro, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    registerButton.isEnabled = true
                }
            }
        }
    }
}