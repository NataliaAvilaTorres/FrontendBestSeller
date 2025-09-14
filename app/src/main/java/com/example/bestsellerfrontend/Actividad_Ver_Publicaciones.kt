package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Actividad_Ver_Publicaciones : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var publicaciones: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_ver_publicaciones, container, false)

        // Botón regresar
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerMisPublicaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = OfertaAdaptador(emptyList())
        recyclerView.adapter = adapter

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // emulador
            //.baseUrl("http://10.195.48.116:8090/") // cambia si usas emulador con 10.0.2.2
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Cargar publicaciones del usuario
        lifecycleScope.launch {
            try {
                publicaciones = apiService.listarOfertas() // Aquí puedes filtrar solo las del usuario
                adapter.actualizarLista(publicaciones)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }
}