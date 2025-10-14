package com.example.bestsellerfrontend

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*


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
    private lateinit var btnCerca: ImageView
    private var productos: List<Producto> = emptyList()
    private var listaCompletaTiendas: List<Tienda> = emptyList()
    private var categoriaSeleccionada: String? = null
    private var tiendaSeleccionadaId: String? = null
    private var textoBusquedaProducto: String? = null
    private var ordenAscendente: Boolean? = null
    private lateinit var fusedClient: FusedLocationProviderClient

    // --- Permisos de ubicación ---
    private val locationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            obtenerUbicacionYOrdenar()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT)
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_productos, container, false)

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- Inicializar cliente de ubicación ---
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // --- Inicializar RecyclerView de categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias2)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Lista de categorías disponibles
        val categorias = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantáneos"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Pastas y Harinas")
        )

        // Adaptador para categorías
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

        // --- Inicializar RecyclerView de tiendas ---
        recyclerViewTiendas = view.findViewById(R.id.recyclerViewTiendas)
        recyclerViewTiendas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Adaptador de tiendas con callback al hacer clic
        adapterTiendas = TiendaAdaptador(
            listaTiendas = emptyList(),
            context = requireContext(),
            layoutId = R.layout.item_tienda
        ) { tienda ->
            tiendaSeleccionadaId = tienda.id
            Toast.makeText(requireContext(), "Seleccionaste ${tienda.nombre}", Toast.LENGTH_SHORT)
                .show()
            aplicarFiltros()
        }
        recyclerViewTiendas.adapter = adapterTiendas

        // --- Barra de búsqueda de tiendas ---
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

        // --- Barra de búsqueda de productos ---
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

        // --- Coincidencias y botones de orden ---
        textViewCoincidencias = view.findViewById(R.id.textViewCoincidencias)
        btnOrdenAsc = view.findViewById(R.id.btnOrdenAsc)
        btnOrdenDesc = view.findViewById(R.id.btnOrdenDesc)
        btnOrdenAsc.setOnClickListener { ordenAscendente = true; aplicarFiltros() }
        btnOrdenDesc.setOnClickListener { ordenAscendente = false; aplicarFiltros() }

        // --- Botón para ordenar tiendas por cercanía ---
        btnCerca = view.findViewById(R.id.btnCerca)
        btnCerca.setOnClickListener { pedirUbicacionYOrdenarTiendas() }

        // --- Configuración de Retrofit (API REST) ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // Dirección local del backend (emulador)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- RecyclerView de productos ---
        recyclerViewProductos = view.findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = LinearLayoutManager(requireContext())
        adapterProductos = ProductoAdaptador(emptyList(), listaCompletaTiendas)
        recyclerViewProductos.adapter = adapterProductos

        // --- Cargar datos desde la API ---
        lifecycleScope.launch {
            try {
                // Primero cargamos las tiendas
                val tiendas = apiService.listarTiendas()
                listaCompletaTiendas = tiendas
                adapterTiendas.actualizarLista(tiendas)

                // Luego los productos (para poder asociarlos con sus tiendas)
                cargarProductos()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar tiendas", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        return view
    }

    /** Carga los productos desde la API */
    private fun cargarProductos() {
        lifecycleScope.launch {
            try {
                productos = apiService.listarProductos()
                aplicarFiltros()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar productos", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /** Aplica todos los filtros combinados: categoría, tienda, búsqueda y orden */
    private fun aplicarFiltros() {
        var productosFiltrados = productos

        categoriaSeleccionada?.let { categoria ->
            productosFiltrados = productosFiltrados.filter {
                it.marca.categoria.equals(categoria, ignoreCase = true)
            }
        }

        tiendaSeleccionadaId?.let { idTienda ->
            productosFiltrados = productosFiltrados.filter { it.tiendaId == idTienda }
        }

        textoBusquedaProducto?.let { texto ->
            val busqueda = texto.lowercase()
            productosFiltrados = productosFiltrados.filter {
                it.nombre.lowercase().contains(busqueda)
            }
        }

        ordenAscendente?.let { asc ->
            productosFiltrados = if (asc) {
                productosFiltrados.sortedBy { it.precio }
            } else {
                productosFiltrados.sortedByDescending { it.precio }
            }
        }

        adapterProductos = ProductoAdaptador(productosFiltrados, listaCompletaTiendas)
        recyclerViewProductos.adapter = adapterProductos

        textViewCoincidencias.text = "${productosFiltrados.size} coincidencias"
    }

    /** Filtra las tiendas por nombre según el texto ingresado */
    private fun filtrarTiendas(query: String?) {
        val texto = query?.lowercase() ?: ""
        val filtradas = listaCompletaTiendas.filter {
            it.nombre.lowercase().contains(texto)
        }
        adapterTiendas.actualizarLista(filtradas)
    }


    /** Solicita permisos de ubicación al usuario */
    private fun pedirUbicacionYOrdenarTiendas() {
        locationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /** Obtiene la ubicación actual del usuario y ordena las tiendas por cercanía */
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionYOrdenar() {
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    ordenarTiendasPorDistancia(loc)
                } else {
                    // Si no hay ubicación actual, intentamos obtener la última conocida
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) ordenarTiendasPorDistancia(last)
                            else Toast.makeText(
                                requireContext(),
                                "No se pudo obtener ubicación",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Error de ubicación: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Error de ubicación: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /** Ordena las tiendas en función de la distancia al usuario */
    private fun ordenarTiendasPorDistancia(ubicacion: Location) {
        if (listaCompletaTiendas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay tiendas para ordenar", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val latUser = ubicacion.latitude
        val lngUser = ubicacion.longitude

        // Calculamos la distancia a cada tienda
        val tiendasConDist = listaCompletaTiendas
            .filter { it.ubicacion != null }
            .map { tienda ->
                val distKm = distanciaKm(
                    latUser, lngUser,
                    tienda.ubicacion!!.lat, tienda.ubicacion.lng
                )
                Pair(tienda, distKm)
            }
            .sortedBy { it.second }

        if (tiendasConDist.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Las tiendas no tienen coordenadas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Tomamos las 20 más cercanas
        val top = tiendasConDist.take(20).map { it.first }
        adapterTiendas.actualizarLista(top)

        // Mostramos la más cercana al usuario
        val primera = tiendasConDist.first()
        Toast.makeText(
            requireContext(),
            "Más cercana: ${primera.first.nombre} (${formatKm(primera.second)})",
            Toast.LENGTH_SHORT
        ).show()
    }

    /** Calcula la distancia entre dos puntos geográficos usando la fórmula Haversine */
    private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /** Formatea la distancia mostrando metros o kilómetros según corresponda */
    private fun formatKm(km: Double): String {
        return if (km < 1) "${(km * 1000).roundToInt()} m" else String.format("%.2f km", km)
    }
}