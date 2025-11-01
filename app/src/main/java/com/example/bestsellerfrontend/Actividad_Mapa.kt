package com.example.bestsellerfrontend

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.util.*

// NUEVOS imports para el InfoWindow con imagen
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // APIs
    private val apiKey = "AIzaSyAmk_pwGdekb606Okhp9tCKKw5o3XiG4Ic"
    private lateinit var directionsService: ApiService
    private lateinit var backendService: ApiService

    // Autocomplete
    private var autocompleteOrigen: AutocompleteSupportFragment? = null
    private var autocompleteDestino: AutocompleteSupportFragment? = null

    // Marcadores / ruta
    private var markerOrigen: Marker? = null
    private var markerDestino: Marker? = null
    private var polylineRuta: Polyline? = null

    // Estado
    private var origenLatLng: LatLng? = null
    private var destinoLatLng: LatLng? = null
    private var direccionDestino: String? = null

    // Tiendas backend
    private var tiendas: List<Tienda> = emptyList()

    // CACHE de bitmaps para las imágenes en el InfoWindow
    private val markerIconCache = mutableMapOf<Marker, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lee argumentos desde OfertaAdaptador
        arguments?.let { args ->
            if (args.containsKey("destino_lat") && args.containsKey("destino_lng")) {
                val lat = args.getDouble("destino_lat")
                val lng = args.getDouble("destino_lng")
                destinoLatLng = LatLng(lat, lng)
            }
            direccionDestino = args.getString("destino_direccion")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_mapa, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (!Places.isInitialized()) Places.initialize(requireContext(), apiKey)

        // Mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.map, it).commit()
            }
        mapFragment.getMapAsync(this)

        // Retrofit Google (Directions)
        directionsService = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        // Retrofit backend (tus tiendas)
        backendService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        inicializarAutocomplete()

        return view
    }

    private fun inicializarAutocomplete() {
        autocompleteOrigen = childFragmentManager
            .findFragmentById(R.id.autocomplete_origen) as AutocompleteSupportFragment
        autocompleteDestino = childFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteOrigen?.setHint("Selecciona tu origen")
        autocompleteDestino?.setHint("Selecciona tu destino")

        val campos = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        autocompleteOrigen?.setPlaceFields(campos)
        autocompleteDestino?.setPlaceFields(campos)

        autocompleteOrigen?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    origenLatLng = it
                    actualizarMarkerOrigen(it, "Origen: ${place.name}")
                    dibujarRuta() // por si ya hay destino
                }
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {}
        })

        autocompleteDestino?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    destinoLatLng = it
                    actualizarMarkerDestino(it, "Destino: ${place.name}")
                    dibujarRuta()
                }
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {}
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Adapter del InfoWindow (nombre + imagen + "Ver productos")
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                // Usamos el "globo" por defecto; personalizamos el contenido.
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                val tienda = marker.tag as? Tienda ?: return null
                val view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.info_window_tienda, null, false)

                val img = view.findViewById<ImageView>(R.id.imgTiendaInfo)
                val tv = view.findViewById<TextView>(R.id.tvNombreTiendaInfo)
                val btn = view.findViewById<TextView>(R.id.btnVerProductosInfo)

                tv.text = tienda.nombre
                btn.text = "Ver productos"

                // Usa cache si ya está el bitmap
                val bmp = markerIconCache[marker]
                if (bmp != null) {
                    img.setImageBitmap(bmp)
                } else {
                    img.setImageResource(R.drawable.fondo_imagen_redonda)
                }
                return view
            }
        })

        // Click en todo el InfoWindow -> abrir ListaProductosFragment filtrado por tienda
        mMap.setOnInfoWindowClickListener { marker ->
            val tienda = marker.tag as? Tienda ?: return@setOnInfoWindowClickListener
            val frag = ListaProductosFragment().apply {
                arguments = Bundle().apply {
                    putString("filtro_tienda_id", tienda.id)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, frag)
                .addToBackStack(null)
                .commit()
        }

        // Si vino destino por argumentos, colócalo
        destinoLatLng?.let {
            actualizarMarkerDestino(it, "Destino: ${direccionDestino ?: "Destino"}")
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            direccionDestino?.let { dir -> autocompleteDestino?.setText(dir) }
        }

        // Permisos ubicación
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }

        // Carga y pinta SOLO tus tiendas (pines rojos)
        cargarTiendasYMarcarlas()
    }

    private fun actualizarMarkerDestino(pos: LatLng, titulo: String) {
        markerDestino?.remove()
        markerDestino = mMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(titulo)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    @SuppressLint("MissingPermission")
    private fun habilitarUbicacion() {
        try {
            mMap.isMyLocationEnabled = true
            obtenerUbicacionActual()
        } catch (_: SecurityException) {}
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val pos = LatLng(location.latitude, location.longitude)
                origenLatLng = pos
                actualizarMarkerOrigen(pos, "Tu ubicación")

                // Autocomplete de origen con dirección legible
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val dirs = geocoder.getFromLocation(pos.latitude, pos.longitude, 1)
                if (!dirs.isNullOrEmpty()) autocompleteOrigen?.setText(dirs[0].getAddressLine(0))

                // Si ya hay destino (por argumento o por búsqueda), dibuja la ruta
                dibujarRuta()
            }
        }
    }

    private fun actualizarMarkerOrigen(pos: LatLng, titulo: String) {
        markerOrigen?.remove()
        markerOrigen = mMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(titulo)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        if (destinoLatLng == null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        }
    }

    // ===== SOLO TIENDAS DEL BACKEND =====
    private fun cargarTiendasYMarcarlas() {
        lifecycleScope.launch {
            try {
                tiendas = backendService.listarTiendas()
                pintarMarcadoresDeTiendas(tiendas)
            } catch (_: Exception) {}
        }
    }

    private fun pintarMarcadoresDeTiendas(tiendas: List<Tienda>) {
        val bounds = LatLngBounds.Builder()
        var anyAdded = false

        tiendas.forEach { tienda ->
            val u = tienda.ubicacion
            if (u != null) {
                val pos = LatLng(u.lat, u.lng)
                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(tienda.nombre)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                if (marker != null) {
                    // Relaciona el marker con la tienda (lo usamos en el InfoWindow y el click)
                    marker.tag = tienda

                    // Precarga la imagen como Bitmap para el InfoWindow
                    if (tienda.urlImagen.isNotEmpty()) {
                        Glide.with(this)
                            .asBitmap()
                            .load(tienda.urlImagen)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    markerIconCache[marker] = resource
                                    if (marker.isInfoWindowShown) marker.showInfoWindow()
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })
                    }
                }

                bounds.include(pos)
                anyAdded = true
            }
        }

        if (anyAdded && destinoLatLng == null) {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
            } catch (_: Exception) {}
        }
    }

    // ===== RUTA =====
    private fun dibujarRuta() {
        val o = origenLatLng ?: return
        val d = destinoLatLng ?: return

        lifecycleScope.launch {
            try {
                val url =
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=${o.latitude},${o.longitude}" +
                            "&destination=${d.latitude},${d.longitude}" +
                            "&key=$apiKey"

                val result = withContext(Dispatchers.IO) { URL(url).readText() }
                val json = JSONObject(result)
                val routes = json.optJSONArray("routes")
                if (routes == null || routes.length() == 0) return@launch

                val puntos = routes
                    .getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val decoded = decodePolyline(puntos)
                polylineRuta?.remove()
                polylineRuta = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(decoded)
                        .color(android.graphics.Color.BLUE)
                        .width(10f)
                )
            } catch (_: Exception) {}
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    // Permisos
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) habilitarUbicacion()
    }
}
