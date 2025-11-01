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
    private lateinit var productName: TextView
    private lateinit var productCategory: TextView
    private lateinit var productImage: ImageView
    private lateinit var chipPrecio: TextView
    private lateinit var chipTienda: TextView
    private lateinit var chipLikes: TextView

    // --- Datos y servicios ---
    private lateinit var apiService: ApiService
    private var ofertasSimilares: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.actividad_vista_detalles_producto, container, false)

        // Inicialización de vistas
        productName = view.findViewById(R.id.productName)
        productCategory = view.findViewById(R.id.productCategory)
        productImage = view.findViewById(R.id.productImage)
        chipPrecio = view.findViewById(R.id.productPrice)
        chipTienda = view.findViewById(R.id.productStore)
        chipLikes = view.findViewById(R.id.productlikes)
        recyclerViewSimilares = view.findViewById(R.id.recyclerSimilares)

        recyclerViewSimilares.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Configurar Retrofit (API)
        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.0.2.2:8090/")
            .baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Configurar adaptador del RecyclerView
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewSimilares.adapter = adapter

        // Botón de regreso
        view.findViewById<ImageView>(R.id.btnRegresar).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Recibir argumentos del producto seleccionado
        val productoIdArg = arguments?.getString("producto_id")
        val productoNombre = arguments?.getString("producto_nombre") ?: ""
        val productoCategoria = arguments?.getString("producto_categoria") ?: ""
        val productoPrecio = arguments?.getDouble("producto_precio") ?: 0.0
        val productoImagen = arguments?.getString("producto_imagen") ?: ""

        // Mostrar datos básicos en la interfaz
        productName.text = productoNombre
        productCategory.text = productoCategoria
        chipPrecio.text = "$ ${"%,.0f".format(productoPrecio)}"

        // Cargar imagen con Glide (si existe)
        if (productoImagen.isNotEmpty()) {
            Glide.with(this).load(productoImagen).into(productImage)
        }

        // Cargar datos adicionales (likes, tienda y similares)
        lifecycleScope.launch {
            try {
                // Llamadas paralelas para mejorar rendimiento
                val ofertasDefer = async { apiService.listarOfertas() }
                val productosDefer = async { apiService.listarProductos() }

                val todasLasOfertas = ofertasDefer.await()
                val todosLosProductos = productosDefer.await()

                // Buscar el producto actual
                val productoActual = when {
                    !productoIdArg.isNullOrBlank() ->
                        todosLosProductos.firstOrNull { it.id == productoIdArg }
                    else ->
                        todosLosProductos.firstOrNull { it.nombre.equals(productoNombre, true) }
                }

                // Obtener número de likes
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


                // Obtener nombre de tienda
                val tiendaNombre = productoActual?.tiendaId?.let { tid ->
                    runCatching { apiService.obtenerTienda(tid).nombre }.getOrNull()
                        ?: runCatching {
                            apiService.listarTiendas().firstOrNull { it.id == tid }?.nombre
                        }.getOrNull()
                }
                chipTienda.text = tiendaNombre ?: "Desconocida"

                // Buscar ofertas de productos similares
                val ofertasConProducto = todasLasOfertas.mapNotNull { oferta ->
                    val prod = todosLosProductos.find { it.id == oferta.productoId }
                    prod?.let { Pair(oferta, prod) }
                }

                // Filtrar ofertas con la misma categoría
                val ofertasFiltradas = ofertasConProducto
                    .filter { (_, prod) ->
                        prod.marca.categoria.equals(productoCategoria, ignoreCase = true) &&
                                prod.nombre != productoNombre
                    }
                    .map { (oferta, _) -> oferta }

                // Actualizar adaptador
                ofertasSimilares = ofertasFiltradas
                adapter.actualizarLista(ofertasFiltradas)

                // Ocultar el RecyclerView si no hay similares
                recyclerViewSimilares.visibility =
                    if (ofertasFiltradas.isEmpty()) View.GONE else View.VISIBLE

            } catch (e: Exception) {
                // Manejo de errores
                e.printStackTrace()
                chipTienda.text = "Desconocida"
                chipLikes.text = "0"
            }
        }

        return view
    }
}