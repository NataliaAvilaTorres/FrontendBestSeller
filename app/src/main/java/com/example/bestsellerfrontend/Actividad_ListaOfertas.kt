package com.example.bestsellerfrontend

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class ListaOfertasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    // Tiendas para proximidad
    private var tiendas: List<Tienda> = emptyList()

    // Spinners
    private lateinit var spinnerLikes: Spinner
    private lateinit var spinnerExpiracion: Spinner
    private lateinit var spinnerProximidad: Spinner

    // Ubicación
    private lateinit var fusedClient: FusedLocationProviderClient
    private var ultimaUbicacion: Location? = null

    private val locationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            obtenerUbicacionYOrdenarPorProximidad(asc = true) // por defecto más cerca primero
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            // Revertimos selección si el usuario eligió proximidad
            spinnerProximidad.setSelection(0) // "Sin orden"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_ofertas, container, false)

        // Botón regresar
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Botón agregar
        val btnAdd = view.findViewById<ImageButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, FormularioNuevaOfertaFragment())
                .addToBackStack(null)
                .commit()
        }

        // Ubicación
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = false)
        recyclerView.adapter = adapter

        // Carga de datos
        lifecycleScope.launch {
            try {
                // Primero tiendas (para proximidad)
                tiendas = apiService.listarTiendas()
                // Luego ofertas
                ofertas = apiService.listarOfertas()
                adapter.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // === Spinners ===
        spinnerLikes = view.findViewById(R.id.btnLikes)
        spinnerExpiracion = view.findViewById(R.id.spinnerExpiracion)
        spinnerProximidad = view.findViewById(R.id.spinnerProximidad)

        // Likes
        val opcionesLikes = listOf("Likes", "Menos a más likes", "Más a menos likes")
        spinnerLikes.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesLikes).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Expiración (usa fechaFinal)
        val opcionesExp = listOf("Expiración", "Expira más pronto", "Expira más tarde")
        spinnerExpiracion.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesExp).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Proximidad
        val opcionesProx = listOf("Proximidad", "Más cerca primero", "Más lejos primero")
        spinnerProximidad.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesProx).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Listeners
        spinnerLikes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (ofertas.isEmpty()) return
                val lista = when (pos) {
                    1 -> ofertas.sortedBy { it.likes }            // asc
                    2 -> ofertas.sortedByDescending { it.likes }  // desc
                    else -> ofertas
                }
                adapter.actualizarLista(lista)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerExpiracion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (ofertas.isEmpty()) return
                val lista = when (pos) {
                    1 -> ofertas.sortedBy { it.fechaFinal }            // expira primero
                    2 -> ofertas.sortedByDescending { it.fechaFinal }  // expira último
                    else -> ofertas
                }
                adapter.actualizarLista(lista)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerProximidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (ofertas.isEmpty()) return
                when (pos) {
                    1 -> pedirPermisosYOrdenarProximidad(asc = true)   // más cerca primero
                    2 -> pedirPermisosYOrdenarProximidad(asc = false)  // más lejos primero
                    else -> adapter.actualizarLista(ofertas)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }

    private fun pedirPermisosYOrdenarProximidad(asc: Boolean) {
        locationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        // El orden real se hará en el callback si se conceden
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionYOrdenarPorProximidad(asc: Boolean) {
        // Si ya tenemos última ubicación reciente, úsala
        ultimaUbicacion?.let { ordenarPorProximidad(it, asc); return }

        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    ultimaUbicacion = loc
                    ordenarPorProximidad(loc, asc)
                } else {
                    // Fallback
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) {
                                ultimaUbicacion = last
                                ordenarPorProximidad(last, asc)
                            } else {
                                Toast.makeText(requireContext(), "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
                                spinnerProximidad.setSelection(0)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error de ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                            spinnerProximidad.setSelection(0)
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error de ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                spinnerProximidad.setSelection(0)
            }
    }

    private fun ordenarPorProximidad(ubicacion: Location, asc: Boolean) {
        if (tiendas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay tiendas para calcular distancia", Toast.LENGTH_SHORT).show()
            spinnerProximidad.setSelection(0)
            return
        }

        val latUser = ubicacion.latitude
        val lngUser = ubicacion.longitude

        // Mapa rápido tiendaId -> Tienda
        val mapaTiendas = tiendas.associateBy { it.id }

        val ofertasConDist = ofertas.map { oferta ->
            val tienda = mapaTiendas[oferta.tiendaId]
            val distKm = if (tienda?.ubicacion != null) {
                distanciaKm(
                    latUser, lngUser,
                    tienda.ubicacion.lat, tienda.ubicacion.lng
                )
            } else {
                Double.POSITIVE_INFINITY // sin coords: al final
            }
            oferta to distKm
        }

        val ordenadas = if (asc) {
            ofertasConDist.sortedBy { it.second }.map { it.first }
        } else {
            ofertasConDist.sortedByDescending { it.second }.map { it.first }
        }

        adapter.actualizarLista(ordenadas)

        val primero = ofertasConDist.minByOrNull { it.second }
        primero?.let {
            if (it.second.isFinite()) {
                Toast.makeText(
                    requireContext(),
                    "Oferta más cercana: ${it.first.nombreOferta} (${formatKm(it.second)})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // --- utilidades distancia ---
    private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun formatKm(km: Double): String {
        return if (km < 1) "${(km * 1000).roundToInt()} m" else String.format("%.2f km", km)
    }
}
