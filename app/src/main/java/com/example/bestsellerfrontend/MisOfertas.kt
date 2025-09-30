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

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        recyclerView = view.findViewById(R.id.recyclerViewMisOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = true)
        recyclerView.adapter = adapter

        val prefs = requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioId = prefs.getString("id", null)

        //  Llamar al endpoint de tus ofertas
        if (usuarioId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    ofertas = apiService.listarOfertasUsuario(usuarioId)
                    adapter.actualizarLista(ofertas)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return view
    }
}
