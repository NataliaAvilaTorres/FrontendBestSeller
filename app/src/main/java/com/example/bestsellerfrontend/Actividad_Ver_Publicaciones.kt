package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.TextView
import android.widget.Toast


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

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- CONFIGURAR RECYCLER VIEW ---
        recyclerView = view.findViewById(R.id.recyclerMisPublicaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // --- CONFIGURAR RETROFIT ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // ✅ IMPORTANTE: mostrarBotones = true para que aparezcan botones de Editar/Eliminar
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = true)
        recyclerView.adapter = adapter

        // --- OBTENER ID DEL USUARIO ---
        val prefs = requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioId = prefs.getString("id", null)

        // --- TEXTVIEWS PARA ESTADÍSTICAS ---
        val tvNumeroPublicaciones = view.findViewById<TextView>(R.id.tvNumeroPublicaciones)
        val tvLikesRecibidos = view.findViewById<TextView>(R.id.tvLikesRecibidos)

        // --- CARGAR PUBLICACIONES DEL USUARIO ---
        if (usuarioId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    publicaciones = apiService.listarOfertasUsuario(usuarioId)
                    adapter.actualizarLista(publicaciones)

                    // Actualizar estadísticas
                    tvNumeroPublicaciones.text = publicaciones.size.toString()
                    val totalLikes = publicaciones.sumOf { it.likes }
                    tvLikesRecibidos.text = totalLikes.toString()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}