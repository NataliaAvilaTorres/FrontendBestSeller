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

class ListaProductosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdaptador
    private lateinit var apiService: ApiService
    private var productos: List<Producto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_lista_productos, container, false)

        val btnCategoria = view.findViewById<Button>(R.id.btnCategoria)
        btnCategoria.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, VistaCategoriasFragment())
                .addToBackStack(null)
                .commit()
        }

        recyclerView = view.findViewById(R.id.recyclerViewProductos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProductoAdaptador(emptyList())
        recyclerView.adapter = adapter

        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.0.2.2:8090/")
            .baseUrl("http://10.195.48.116:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        val categoriaFiltro = arguments?.getString("categoria_filtro")

        lifecycleScope.launch {
            try {
                productos = apiService.listarProductos()

                val listaFiltrada = if (categoriaFiltro != null) {
                    productos.filter { it.categoria.equals(categoriaFiltro, ignoreCase = true) }
                } else {
                    productos
                }

                adapter.actualizarLista(listaFiltrada)

                productos = listaFiltrada

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Configuración de Spinners
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

        // Listener del Spinner A-Z
        spinnerOrdenAZ.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (productos.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> productos.sortedBy { it.nombre }
                        1 -> productos.sortedByDescending { it.nombre }
                        else -> productos
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener del Spinner Precio
        spinnerPrecio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (productos.isNotEmpty()) {
                    val listaOrdenada = when (position) {
                        0 -> productos.sortedBy { it.precio }
                        1 -> productos.sortedByDescending { it.precio }
                        else -> productos
                    }
                    adapter.actualizarLista(listaOrdenada)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }
}