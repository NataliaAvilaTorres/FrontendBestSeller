package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetalleProductoFragment : Fragment() {

    private lateinit var recyclerViewSimilares: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertasSimilares: List<Oferta> = emptyList()

    private lateinit var productName: TextView
    private lateinit var productCategory: TextView
    private lateinit var productImage: ImageView
    private lateinit var chipPrecio: TextView
    private lateinit var chipTienda: TextView
    private lateinit var chipLikes: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.actividad_vista_detalles_producto, container, false)

        // UI
        productName = view.findViewById(R.id.productName)
        productCategory = view.findViewById(R.id.productCategory)
        productImage = view.findViewById(R.id.productImage)
        chipPrecio = view.findViewById(R.id.productPrice)
        chipTienda = view.findViewById(R.id.productStore)
        chipLikes = view.findViewById(R.id.productlikes)

        recyclerViewSimilares = view.findViewById(R.id.recyclerSimilares)
        recyclerViewSimilares.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Inicializar Retrofit antes de usar adaptador
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.7:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewSimilares.adapter = adapter

        view.findViewById<ImageView>(R.id.btnRegresar).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Recuperar datos de la oferta enviados desde el adaptador
        val productoNombre = arguments?.getString("producto_nombre") ?: ""
        val productoCategoria = arguments?.getString("producto_categoria") ?: ""
        val productoPrecio = arguments?.getDouble("producto_precio") ?: 0.0
        val productoImagen = arguments?.getString("producto_imagen") ?: ""
        val ofertaTienda = arguments?.getString("oferta_tienda") ?: "Desconocida"
        val ofertaLikes = arguments?.getInt("oferta_likes") ?: 0

        // Actualizar UI
        productName.text = productoNombre
        productCategory.text = productoCategoria
        chipPrecio.text = "$ ${"%,.0f".format(productoPrecio)}"
        chipTienda.text = ofertaTienda
        chipLikes.text = ofertaLikes.toString()

        // Cargar la imagen del producto con Glide
        if (productoImagen.isNotEmpty()) {
            Glide.with(this)
                .load(productoImagen)
                .into(productImage)
        }

        // Cargar ofertas similares desde backend
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val todasLasOfertas = apiService.listarOfertas()
                ofertasSimilares = todasLasOfertas.filter {
                    it.producto.categoria == productoCategoria &&
                            it.producto.nombre != productoNombre
                }
                adapter.actualizarLista(ofertasSimilares)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return view
    }
}