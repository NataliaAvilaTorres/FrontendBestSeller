package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ImageView

class ListaProductosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()  // lista de ofertas ahora

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_productos, container, false)

        // --- BOTÓN REGRESAR ---
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- BOTÓN CATEGORÍAS ---
        val btnCategoria = view.findViewById<Button>(R.id.btnCategoria)
        btnCategoria.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, VistaCategoriasFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- RECYCLER VIEW ---
        recyclerView = view.findViewById(R.id.recyclerViewProductos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProductoAdaptador(emptyList())  // ahora recibe ofertas
        recyclerView.adapter = adapter

        // --- RETROFIT ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.7:8090/") // tu backend real
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val categoriaFiltro = arguments?.getString("categoria_filtro")

        // --- CARGA DE DATOS ---
        lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()

                val listaFiltrada = if (categoriaFiltro != null) {
                    ofertas.filter { it.producto.categoria.equals(categoriaFiltro, ignoreCase = true) }
                } else {
                    ofertas
                }

                adapter.actualizarLista(listaFiltrada)
                ofertas = listaFiltrada

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // =========================
        // Configuración de Spinners
        // =========================

        // Spinner A-Z
        val spinnerOrdenAZ = view.findViewById<android.widget.Spinner>(R.id.spinnerOrdenAZ)
        val opcionesAZ = listOf("A-Z", "Z-A")
        val adapterAZ = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesAZ)
        adapterAZ.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerOrdenAZ.adapter = adapterAZ

        // Spinner Precio
        val spinnerPrecio = view.findViewById<android.widget.Spinner>(R.id.spinnerPrecio)
        val opcionesPrecio = listOf("Menor a mayor", "Mayor a menor")
        val adapterPrecio = ArrayAdapter(requireContext(), R.layout.spinner_item, opcionesPrecio)
        adapterPrecio.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerPrecio.adapter = adapterPrecio

        // Listener del Spinner A-Z
        spinnerOrdenAZ.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (ofertas.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> ofertas.sortedBy { it.producto.nombre }
                        1 -> ofertas.sortedByDescending { it.producto.nombre }
                        else -> ofertas
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener del Spinner Precio
        spinnerPrecio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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

        return view
    }
}