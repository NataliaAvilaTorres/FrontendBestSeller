package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        // Botón regresar
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Botón agregar oferta
        val btnAdd = view.findViewById<android.widget.ImageButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, FormularioNuevaOfertaFragment())
                .addToBackStack(null)
                .commit()
        }

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // emulador Android
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = false)
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
        val adapterAZ = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesAZ)
        adapterAZ.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerOrdenAZ.adapter = adapterAZ

        val spinnerPrecio = view.findViewById<android.widget.Spinner>(R.id.spinnerPrecio)
        val opcionesPrecio = listOf("Menor a mayor", "Mayor a menor")
        val adapterPrecio = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesPrecio)
        adapterPrecio.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerPrecio.adapter = adapterPrecio

        val spinnerLikes = view.findViewById<android.widget.Spinner>(R.id.btnLikes)
        val opcionesLikes = listOf("Menos a más likes", "Más a menos likes")
        val adapterLikes = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesLikes)
        adapterLikes.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerLikes.adapter = adapterLikes


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


        spinnerLikes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.likes }
                        1 -> ofertas.sortedByDescending { it.likes }
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