package com.example.bestsellerfrontend

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Retrofit
import androidx.lifecycle.lifecycleScope
import retrofit2.converter.gson.GsonConverterFactory
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import kotlinx.coroutines.launch

class DetalleProductoFragment : Fragment() {

    private lateinit var recyclerViewSimilares: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertasSimilares: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_vista_detalles_producto, container, false)

        val productName = view.findViewById<TextView>(R.id.productName)
        val productCategory = view.findViewById<TextView>(R.id.productCategory)
        val productImage = view.findViewById<ImageView>(R.id.productImage)
        val chipPrecio = view.findViewById<TextView>(R.id.productPrice)
        val chipTienda = view.findViewById<TextView>(R.id.productStore)

        recyclerViewSimilares = view.findViewById(R.id.recyclerSimilares)
        recyclerViewSimilares.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter = OfertaAdaptador(emptyList())
        recyclerViewSimilares.adapter = adapter

        // Recuperar los argumentos enviados desde el adaptador
        val nombre = arguments?.getString("producto_nombre")
        val categoria = arguments?.getString("producto_categoria")
        val precio = arguments?.getDouble("producto_precio", 0.0)
        val tienda = arguments?.getString("producto_marca") ?: "Desconocida"
        val imagenUrl = arguments?.getString("producto_imagen")

        productName.text = nombre
        productCategory.text = categoria
        chipPrecio.text = "$ ${"%,.0f".format(precio)}"
        chipTienda.text = tienda

        Glide.with(this).load(imagenUrl).into(productImage)

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val todasLasOfertas = apiService.listarOfertas()
                ofertasSimilares = todasLasOfertas.filter { it.producto.categoria == categoria }
                adapter.actualizarLista(ofertasSimilares)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }
}
