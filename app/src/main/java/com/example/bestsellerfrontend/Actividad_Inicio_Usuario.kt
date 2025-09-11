package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InicioUsuarioFragment : Fragment() {

    private lateinit var recyclerViewOfertas: RecyclerView
    private lateinit var adapterOfertas: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {

        val view = inflater.inflate(R.layout.actividad_inicio_usuario, container, false)

        // --- RecyclerView de Ofertas ---
        recyclerViewOfertas = view.findViewById(R.id.recyclerViewOfertas)
        recyclerViewOfertas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapterOfertas = OfertaAdaptador(emptyList())
        recyclerViewOfertas.adapter = adapterOfertas

        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.195.48.116:8090/") // tu red local
            .baseUrl("http://10.0.2.2:8090/") // para emulador
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()
                adapterOfertas.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Snacks"),
            Pair(R.drawable.granos, "Lácteos"),
            Pair(R.drawable.dulces, "Carnes")
        )

        adapterCategorias = CategoriaAdaptador(categorias)
        recyclerViewCategorias.adapter = adapterCategorias

        val btnAdd: ImageButton = view.findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, NotificacionesFragment()) // 👈 abre el fragmento
                .addToBackStack(null) // permite volver atrás
                .commit()
        }
        return view



    }
}