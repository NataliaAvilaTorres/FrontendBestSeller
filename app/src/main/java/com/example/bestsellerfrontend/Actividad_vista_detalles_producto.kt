package com.example.bestsellerfrontend

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Actividad_vista_detalles_producto : AppCompatActivity() {

    private lateinit var recyclerViewSimilares: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertasSimilares: List<Oferta> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_vista_detalles_producto)

        // Referencias
        val productName = findViewById<TextView>(R.id.productName)
        val productCategory = findViewById<TextView>(R.id.productCategory)
        val productImage = findViewById<ImageView>(R.id.productImage)
        val chipPrecio = findViewById<TextView>(R.id.productPrice)
        val chipTienda = findViewById<TextView>(R.id.productStore)

        // RecyclerView de productos similares
        recyclerViewSimilares = findViewById(R.id.recyclerSimilares)
        recyclerViewSimilares.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = OfertaAdaptador(emptyList())
        recyclerViewSimilares.adapter = adapter

        // Obtener datos del intent
        val nombre = intent.getStringExtra("producto_nombre")
        val categoria = intent.getStringExtra("producto_categoria")
        val precio = intent.getDoubleExtra("producto_precio", 0.0)
        val tienda = intent.getStringExtra("producto_marca") ?: "Desconocida"
        val imagenUrl = intent.getStringExtra("producto_imagen")

        // Setear datos
        productName.text = nombre
        productCategory.text = categoria
        chipPrecio.text = "$ ${"%,.0f".format(precio)}"
        chipTienda.text = tienda

        Glide.with(this)
            .load(imagenUrl)
            .into(productImage)

        // Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // Cambia si usas servidor externo
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Cargar productos similares
        lifecycleScope.launch {
            try {
                val todasLasOfertas = apiService.listarOfertas()
                ofertasSimilares = todasLasOfertas.filter { it.producto.categoria == categoria }
                adapter.actualizarLista(ofertasSimilares)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}