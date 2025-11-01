package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MisOfertas : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_mis_ofertas, container, false)

        // Configurar Retrofit para conectarse al backend
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crear una instancia del servicio API
        apiService = retrofit.create(ApiService::class.java)

        // Configurar el RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewMisOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Crear el adaptador con lista vacía inicialmente
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = true)
        recyclerView.adapter = adapter

        // Obtener el ID del usuario desde las preferencias compartidas
        val prefs =
            requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioId = prefs.getString("id", null)

        // Si el usuario tiene un ID guardado, obtener sus ofertas desde la API
        if (usuarioId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Llamada a la API para listar las ofertas del usuario
                    ofertas = apiService.listarOfertasUsuario(usuarioId)

                    // Actualizar el adaptador con las ofertas obtenidas
                    adapter.actualizarLista(ofertas)
                } catch (e: Exception) {
                    // Capturar y mostrar cualquier error que ocurra
                    e.printStackTrace()
                }
            }
        }

        // Retornar la vista inflada del fragmento
        return view
    }
}