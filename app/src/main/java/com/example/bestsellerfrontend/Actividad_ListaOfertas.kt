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

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- Botón para agregar una nueva oferta ---
        val btnAdd = view.findViewById<android.widget.ImageButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            // Reemplaza el fragmento actual por el formulario de nueva oferta
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, FormularioNuevaOfertaFragment())
                .addToBackStack(null)
                .commit()
        }

        recyclerView = view.findViewById(R.id.recyclerViewOfertas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // --- Configuración de Retrofit para conectar con la API ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.16:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Inicialización del adaptador ---
        adapter = OfertaAdaptador(
            emptyList(),
            requireContext(),
            apiService,
            mostrarBotones = false
        )
        recyclerView.adapter = adapter

        // --- Carga inicial de ofertas desde la API ---
        lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()
                adapter.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- Spinner para ordenar por nombre (A-Z, Z-A) ---
        val spinnerOrdenAZ = view.findViewById<android.widget.Spinner>(R.id.spinnerOrdenAZ)
        val opcionesAZ = listOf("A-Z", "Z-A")
        val adapterAZ = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesAZ)
        adapterAZ.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerOrdenAZ.adapter = adapterAZ

        // --- Spinner para ordenar por precio ---
        val spinnerPrecio = view.findViewById<android.widget.Spinner>(R.id.spinnerPrecio)
        val opcionesPrecio = listOf("Menor a mayor", "Mayor a menor")
        val adapterPrecio = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesPrecio)
        adapterPrecio.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerPrecio.adapter = adapterPrecio

        // --- Spinner para ordenar por número de likes ---
        val spinnerLikes = view.findViewById<android.widget.Spinner>(R.id.btnLikes)
        val opcionesLikes = listOf("Menos a más likes", "Más a menos likes")
        val adapterLikes = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesLikes)
        adapterLikes.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerLikes.adapter = adapterLikes

        // --- Evento: Ordenar por nombre ---
        spinnerOrdenAZ.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    // Ordena las ofertas según la opción seleccionada
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.nombreOferta }             // A-Z
                        1 -> ofertas.sortedByDescending { it.nombreOferta }  // Z-A
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Evento: Ordenar por número de likes ---
        spinnerLikes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    // Ordena las ofertas según los likes
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.likes }             // Menos a más likes
                        1 -> ofertas.sortedByDescending { it.likes }  // Más a menos likes
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Retorna la vista principal del fragmento
        return view
    }
}