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

        // --- TARJETAS DE CATEGORÍAS ---
        val granos = view.findViewById<CardView>(R.id.granos)
        val bebidas = view.findViewById<CardView>(R.id.bebidas)
        val enlatados = view.findViewById<CardView>(R.id.enlatados)
        val pastas = view.findViewById<CardView>(R.id.pastas)
        val dulces = view.findViewById<CardView>(R.id.dulces)
        val instantaneos = view.findViewById<CardView>(R.id.instantaneos)

        // --- EVENTOS DE CLIC PARA CADA CATEGORÍA ---
        // Cuando el usuario hace clic en una tarjeta, se abre la lista de productos filtrada
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

        // Crea un Bundle (paquete de datos) para enviar la categoría seleccionada
        fragment.arguments = Bundle().apply {
            putString("categoria_filtro", categoria)
        }

        // Reemplaza el fragmento actual por el de lista de productos,
        // manteniendo la navegación en la pila para poder regresar
        parentFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .addToBackStack(null)
            .commit()
    }
}