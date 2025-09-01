package com.example.bestsellerfrontend

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class Actividad_vista_detalles_producto : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_vista_detalles_producto)

        // Referencias
        val productName = findViewById<TextView>(R.id.productName)
        val productCategory = findViewById<TextView>(R.id.productCategory)
        val productImage = findViewById<ImageView>(R.id.productImage)
        val chipPrecio = findViewById<TextView>(R.id.productPrice)
        val chipTienda = findViewById<TextView>(R.id.productStore)

        // Obtener datos del intent
        val nombre = intent.getStringExtra("producto_nombre")
        val categoria = intent.getStringExtra("producto_categoria")
        val precio = intent.getDoubleExtra("producto_precio", 0.0)
        val tienda = intent.getStringExtra("producto_marca") ?: "Desconocida"
        val imagenUrl = intent.getStringExtra("producto_imagen")

        // Asignar valores
        productName.text = nombre
        productCategory.text = categoria
        chipPrecio.text = "$ ${"%,.0f".format(precio)}"
        chipTienda.text = tienda

        // Cargar imagen con Glide
        Glide.with(this)
            .load(imagenUrl)
            .into(productImage)
    }
}