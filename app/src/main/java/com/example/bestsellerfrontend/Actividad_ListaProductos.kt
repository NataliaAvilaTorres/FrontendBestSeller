package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ListaProductosFragment : Fragment() {

    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var recyclerViewTiendas: RecyclerView
    private lateinit var adapterProductos: ProductoAdaptador
    private lateinit var adapterCategorias: CategoriaAdaptador
    private lateinit var adapterTiendas: TiendaAdaptador
    private lateinit var apiService: ApiService
    private lateinit var searchViewTiendas: SearchView
    private lateinit var searchViewProductos: SearchView
    private lateinit var textViewCoincidencias: TextView
    private lateinit var btnOrdenAsc: ImageView
    private lateinit var btnOrdenDesc: ImageView

    private var productos: List<Producto> = emptyList()
    private var listaCompletaTiendas: List<Tienda> = emptyList()

    // Variables para filtros combinados
    private var categoriaSeleccionada: String? = null
    private var tiendaSeleccionadaId: String? = null
    private var textoBusquedaProducto: String? = null
    private var ordenAscendente: Boolean? = null // null = sin ordenar, true = asc, false = desc

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_productos, container, false)

        // --- Botón regresar ---
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- RecyclerView Categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias2)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categorias = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantáneos"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Pastas y Harinas")
        )

        adapterCategorias = CategoriaAdaptador(
            categorias,
            onCategoriaClick = { categoria ->
                categoriaSeleccionada = categoria
                aplicarFiltros()
            },
            clicHabilitado = true,
            layoutId = R.layout.item_categoria
        )
        recyclerViewCategorias.adapter = adapterCategorias

        // --- RecyclerView Tiendas ---
        recyclerViewTiendas = view.findViewById(R.id.recyclerViewTiendas)
        recyclerViewTiendas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        adapterTiendas = TiendaAdaptador(
            listaTiendas = emptyList(),
            context = requireContext(),
            layoutId = R.layout.item_tienda,
            onTiendaClick = { tienda ->
                tiendaSeleccionadaId = tienda.id
                Toast.makeText(requireContext(), "Seleccionaste ${tienda.nombre}", Toast.LENGTH_SHORT).show()
                aplicarFiltros()
            }
        )
        recyclerViewTiendas.adapter = adapterTiendas

        // --- SearchView para tiendas ---
        searchViewTiendas = view.findViewById(R.id.searchviewTiendas)
        searchViewTiendas.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarTiendas(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarTiendas(newText)
                return true
            }
        })

        // --- SearchView para productos ---
        searchViewProductos = view.findViewById(R.id.searchview)
        searchViewProductos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                textoBusquedaProducto = query
                aplicarFiltros()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                textoBusquedaProducto = newText
                aplicarFiltros()
                return true
            }
        })

        // --- Coincidencias y botones de ordenamiento ---
        textViewCoincidencias = view.findViewById(R.id.textViewCoincidencias)
        btnOrdenAsc = view.findViewById(R.id.btnOrdenAsc)
        btnOrdenDesc = view.findViewById(R.id.btnOrdenDesc)

        btnOrdenAsc.setOnClickListener {
            ordenAscendente = true
            aplicarFiltros()
        }

        btnOrdenDesc.setOnClickListener {
            ordenAscendente = false
            aplicarFiltros()
        }

        // --- Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Cargar Tiendas desde API ---
        lifecycleScope.launch {
            try {
                val tiendas = apiService.listarTiendas()
                listaCompletaTiendas = tiendas
                adapterTiendas.actualizarLista(tiendas)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar tiendas", Toast.LENGTH_SHORT).show()
            }
        }

        // --- RecyclerView Productos ---
        recyclerViewProductos = view.findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = LinearLayoutManager(requireContext())
        adapterProductos = ProductoAdaptador(emptyList())
        recyclerViewProductos.adapter = adapterProductos

        cargarProductos()

        return view
    }

    private fun cargarProductos() {
        lifecycleScope.launch {
            try {
                productos = apiService.listarProductos()
                aplicarFiltros() // muestra todos al inicio
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun aplicarFiltros() {
        var productosFiltrados = productos

        categoriaSeleccionada?.let { categoria ->
            productosFiltrados = productosFiltrados.filter {
                it.marca.categoria.equals(categoria, ignoreCase = true)
            }
        }

        tiendaSeleccionadaId?.let { idTienda ->
            productosFiltrados = productosFiltrados.filter {
                it.tiendaId == idTienda
            }
        }

        textoBusquedaProducto?.let { texto ->
            val busqueda = texto.lowercase()
            productosFiltrados = productosFiltrados.filter {
                it.nombre.lowercase().contains(busqueda)
            }
        }

        // Ordenamiento por precio
        ordenAscendente?.let { asc ->
            productosFiltrados = if (asc) {
                productosFiltrados.sortedBy { it.precio }
            } else {
                productosFiltrados.sortedByDescending { it.precio }
            }
        }

        adapterProductos.actualizarLista(productosFiltrados)
    }

    private fun filtrarTiendas(query: String?) {
        val texto = query?.lowercase() ?: ""
        val filtradas = listaCompletaTiendas.filter {
            it.nombre.lowercase().contains(texto)
        }
        adapterTiendas.actualizarLista(filtradas)
    }
}