package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class VistaCategoriasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.actividad_vista_categorias, container, false)

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val granos = view.findViewById<CardView>(R.id.granos) // Granos y Cereales
        val bebidas = view.findViewById<CardView>(R.id.bebidas) // Bebidas
        val enlatados = view.findViewById<CardView>(R.id.enlatados) // Enlatados
        val pastas = view.findViewById<CardView>(R.id.pastas) // Pastas y Harinas
        val dulces = view.findViewById<CardView>(R.id.dulces) // Dulces
        val instantaneos = view.findViewById<CardView>(R.id.instantaneos) // Instantaneos y Precocidos

        granos.setOnClickListener { abrirListaConFiltro("Granos y Cereales") }
        bebidas.setOnClickListener { abrirListaConFiltro("Bebidas") }
        enlatados.setOnClickListener { abrirListaConFiltro("Enlatados") }
        pastas.setOnClickListener { abrirListaConFiltro("Pastas y Harinas") }
        dulces.setOnClickListener { abrirListaConFiltro("Dulces") }
        instantaneos.setOnClickListener { abrirListaConFiltro("Instantaneos y Precocidos") }

        return view
    }

    private fun abrirListaConFiltro(categoria: String) {
        val fragment = ListaProductosFragment()
        fragment.arguments = Bundle().apply {
            putString("categoria_filtro", categoria)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .addToBackStack(null)
            .commit()
    }
}

