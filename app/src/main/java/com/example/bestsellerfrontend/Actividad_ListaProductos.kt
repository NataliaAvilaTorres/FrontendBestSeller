package com.example.bestsellerfrontend

import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.SearchView
import android.widget.EditText
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Actividad_ListaProductos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_listaproductos)

        val spinnerOrdenAZ = findViewById<Spinner>(R.id.spinnerOrdenAZ)

        val opcionesAZ = listOf("A-Z", "Z-A")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesAZ
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrdenAZ.adapter = adapter

        val spinnerPrecio = findViewById<Spinner>(R.id.spinnerPrecio)

        val opcionesPrecio = listOf("Menor a mayor", "Mayor a menor")

        val adapter2 = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesPrecio
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrecio.adapter = adapter2

    }
}