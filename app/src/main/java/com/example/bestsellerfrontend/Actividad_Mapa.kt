package com.example.bestsellerfrontend

import android.Manifest
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

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesService: ApiService
    private val apiKey = "AIzaSyAmk_pwGdekb606Okhp9tCKKw5o3XiG4Ic"

    private var autocompleteOrigen: AutocompleteSupportFragment? = null
    private var autocompleteDestino: AutocompleteSupportFragment? = null
    private var markerOrigen: Marker? = null
    private var markerDestino: Marker? = null
    private var polylineRuta: Polyline? = null

    private var origenLatLng: LatLng? = null
    private var destinoLatLng: LatLng? = null
    private var direccionDestino: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_mapa, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.map, it).commit()
            }
        mapFragment.getMapAsync(this)

        val retrofitPlaces = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        placesService = retrofitPlaces.create(ApiService::class.java)

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
                origenLatLng = place.latLng
                place.latLng?.let {
                    actualizarMarkerOrigen(it, "Origen: ${place.name}")
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                println("Error al seleccionar origen: $status")
            }
        })

        autocompleteDestino?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destinoLatLng = place.latLng
                place.latLng?.let {
                    markerDestino?.remove()
                    markerDestino = mMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Destino: ${place.name}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
                    dibujarRuta()
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                println("Error al seleccionar destino: $status")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        destinoLatLng?.let {
            markerDestino = mMap.addMarker(
                MarkerOptions().position(it).title("Destino: ${direccionDestino ?: "Destino"}")
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun habilitarUbicacion() {
        try {
            mMap.isMyLocationEnabled = true
            obtenerUbicacionActual()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun obtenerUbicacionActual() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val ubicacion = LatLng(location.latitude, location.longitude)
                    origenLatLng = ubicacion
                    actualizarMarkerOrigen(ubicacion, "Tu ubicación")

                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val direcciones =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!direcciones.isNullOrEmpty()) {
                        val direccion = direcciones[0].getAddressLine(0)
                        autocompleteOrigen?.setText(direccion)
                    }

                    buscarSupermercados(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
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
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
    }

    private fun buscarSupermercados(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val respuesta = placesService.buscarLugaresCercanos(
                    ubicacion = "$lat,$lng",
                    radio = 2000,
                    tipo = "supermarket",
                    apiKey = apiKey
                )
                for (lugar in respuesta.results) {
                    val pos = LatLng(lugar.geometry.location.lat, lugar.geometry.location.lng)
                    mMap.addMarker(MarkerOptions().position(pos).title(lugar.name))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dibujarRuta() {
        if (origenLatLng == null || destinoLatLng == null) return

        lifecycleScope.launch {
            try {
                val url =
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=${origenLatLng!!.latitude},${origenLatLng!!.longitude}" +
                            "&destination=${destinoLatLng!!.latitude},${destinoLatLng!!.longitude}" +
                            "&key=$apiKey"

                val result = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }

                val json = JSONObject(result)
                val puntos = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val decodedPath = decodePolyline(puntos)

                polylineRuta?.remove()
                polylineRuta = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPath)
                        .color(android.graphics.Color.BLUE)
                        .width(10f)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        }
    }
}