package com.example.bestsellerfrontend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Actividad_Mapa : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesService: ApiService
    private val apiKey = "AIzaSyAmk_pwGdekb606Okhp9tCKKw5o3XiG4Ic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_mapa)

        // Inicializar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        // Inicializar fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Retrofit para Places API
        val retrofitPlaces = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        placesService = retrofitPlaces.create(ApiService::class.java)

        // Configurar barra de búsqueda (Autocomplete)
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    mMap.addMarker(MarkerOptions().position(it).title(place.name))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))

                    // Buscar supermercados cerca del lugar elegido
                    buscarSupermercados(it.latitude, it.longitude)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                status.statusMessage?.let { println("Error: $it") }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
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
                    mMap.addMarker(MarkerOptions().position(ubicacion).title("Tu ubicación"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f))

                    // Buscar supermercados cerca de la ubicación del usuario
                    buscarSupermercados(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun buscarSupermercados(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val respuesta = placesService.buscarLugaresCercanos(
                    ubicacion = "$lat,$lng",
                    radio = 2000, // 2 km
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        }
    }
}