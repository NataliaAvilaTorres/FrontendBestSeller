package com.example.bestsellerfrontend

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ImageView

class ListaOfertasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_ofertas, container, false)


        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val btnAdd = view.findViewById<android.widget.ImageButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, FormularioNuevaOfertaFragment())
                .addToBackStack(null)
                .commit()
        }

        recyclerView = view.findViewById(R.id.recyclerViewOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.195.48.116:8090/")
            .baseUrl("http://10.0.2.2:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()
                adapter.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val spinnerOrdenAZ = view.findViewById<android.widget.Spinner>(R.id.spinnerOrdenAZ)
        val opcionesAZ = listOf("A-Z", "Z-A")
        val adapterAZ = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcionesAZ)
        adapterAZ.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrdenAZ.adapter = adapterAZ

        val spinnerPrecio = view.findViewById<android.widget.Spinner>(R.id.spinnerPrecio)
        val opcionesPrecio = listOf("Menor a mayor", "Mayor a menor")
        val adapterPrecio = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcionesPrecio)
        adapterPrecio.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrecio.adapter = adapterPrecio

        val spinnerLikes = view.findViewById<android.widget.Spinner>(R.id.btnLikes)
        val opcionesLikes = listOf("Menos a más likes", "Más a menos likes")
        val adapterLikes = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcionesLikes)
        adapterLikes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLikes.adapter = adapterLikes

        // Listener del Spinner A-Z
        spinnerOrdenAZ.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.nombreOferta }
                        1 -> ofertas.sortedByDescending { it.nombreOferta }
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener del Spinner Precio
        spinnerPrecio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.producto.precio }
                        1 -> ofertas.sortedByDescending { it.producto.precio }
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerLikes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.likes }              // Menos a más
                        1 -> ofertas.sortedByDescending { it.likes }   // Más a menos
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }
}