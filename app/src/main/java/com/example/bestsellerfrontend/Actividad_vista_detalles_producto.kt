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
import kotlinx.coroutines.async
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

        // --- UI ---
        productName   = view.findViewById(R.id.productName)
        productCategory = view.findViewById(R.id.productCategory)
        productImage  = view.findViewById(R.id.productImage)
        chipPrecio    = view.findViewById(R.id.productPrice)
        chipTienda    = view.findViewById(R.id.productStore)   // verifica ids en el XML
        chipLikes     = view.findViewById(R.id.productlikes)    // verifica ids en el XML
        recyclerViewSimilares = view.findViewById(R.id.recyclerSimilares)
        recyclerViewSimilares.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // --- Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")  // host de tu PC desde el emulador
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Adapter ---
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewSimilares.adapter = adapter

        // --- Back ---
        view.findViewById<ImageView>(R.id.btnRegresar).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- Args del producto ---
        val productoIdArg    = arguments?.getString("producto_id") // recomendado pasarlo
        val productoNombre   = arguments?.getString("producto_nombre") ?: ""
        val productoCategoria = arguments?.getString("producto_categoria") ?: ""
        val productoPrecio   = arguments?.getDouble("producto_precio") ?: 0.0
        val productoImagen   = arguments?.getString("producto_imagen") ?: ""

        // --- Pintar lo que ya tenemos ---
        productName.text = productoNombre
        productCategory.text = productoCategoria
        chipPrecio.text = "$ ${"%,.0f".format(productoPrecio)}"
        if (productoImagen.isNotEmpty()) {
            Glide.with(this).load(productoImagen).into(productImage)
        }

        // --- Cargar datos faltantes (tienda y likes) + similares ---
        lifecycleScope.launch {
            try {
                // Llamadas en paralelo
                val ofertasDefer   = async { apiService.listarOfertas() }
                val productosDefer = async { apiService.listarProductos() }

                val todasLasOfertas   = ofertasDefer.await()
                val todosLosProductos = productosDefer.await()

                // Resolver producto actual
                val productoActual = when {
                    !productoIdArg.isNullOrBlank() ->
                        todosLosProductos.firstOrNull { it.id == productoIdArg }
                    else ->
                        todosLosProductos.firstOrNull { it.nombre.equals(productoNombre, true) }
                }

                // ---- Likes de la oferta del producto ----
                val likes: Int = productoActual?.id?.let { pid ->
                    val ahora = System.currentTimeMillis()
                    val ofertasDelProducto = todasLasOfertas.filter { it.productoId == pid }
                    val ofertaActiva = ofertasDelProducto.firstOrNull {
                        it.fechaOferta <= ahora && ahora <= it.fechaFinal
                    }
                    val ofertaElegida = ofertaActiva ?: ofertasDelProducto.maxByOrNull { it.fechaOferta }
                    ofertaElegida?.likes
                } ?: 0
                chipLikes.text = likes.toString()

                // ---- Nombre de tienda por tiendaId ----
                val tiendaNombre = productoActual?.tiendaId?.let { tid ->
                    runCatching { apiService.obtenerTienda(tid).nombre }.getOrNull()
                        ?: runCatching {
                            apiService.listarTiendas().firstOrNull { it.id == tid }?.nombre
                        }.getOrNull()
                }
                chipTienda.text = tiendaNombre ?: "Desconocida"

                // ---- Ofertas similares (tu lógica original) ----
                val ofertasConProducto = todasLasOfertas.mapNotNull { oferta ->
                    val prod = todosLosProductos.find { it.id == oferta.productoId }
                    prod?.let { Pair(oferta, prod) }
                }

                val ofertasFiltradas = ofertasConProducto
                    .filter { (_, prod) ->
                        prod.marca.categoria.equals(productoCategoria, ignoreCase = true) &&
                                prod.nombre != productoNombre
                    }
                    .map { (oferta, _) -> oferta }

                ofertasSimilares = ofertasFiltradas
                adapter.actualizarLista(ofertasFiltradas)
                recyclerViewSimilares.visibility =
                    if (ofertasFiltradas.isEmpty()) View.GONE else View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                chipTienda.text = "Desconocida"
                chipLikes.text = "0"
            }
        }

        return view
    }
}
