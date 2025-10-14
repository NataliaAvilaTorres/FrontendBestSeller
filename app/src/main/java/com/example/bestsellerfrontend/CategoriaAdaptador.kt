package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoriaAdaptador(
    private val categorias: List<Pair<Int, String>>,
    private val onCategoriaClick: (String) -> Unit,
    private val clicHabilitado: Boolean = true,
    private val layoutId: Int = R.layout.item_categoria
) : RecyclerView.Adapter<CategoriaAdaptador.CategoriaViewHolder>() {

    // Guarda la categoría actualmente seleccionada
    private var categoriaSeleccionada: String? = null

    // Clase interna que representa cada vista de una categoría en el RecyclerView
    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icono: ImageView = itemView.findViewById(R.id.imgCategoria) // Imagen del ícono
        val nombre: TextView = itemView.findViewById(R.id.txtCategoria) // Texto del nombre
    }

    // Crea una nueva vista para cada ítem del RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return CategoriaViewHolder(view)
    }

    // Asigna los datos (ícono y nombre) a cada vista del RecyclerView
    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val (iconRes, nombreCategoria) = categorias[position]
        holder.icono.setImageResource(iconRes)
        holder.nombre.text = nombreCategoria

        // Ajusta la opacidad de la categoría según si está seleccionada o no
        if (!clicHabilitado) {
            holder.itemView.alpha = 1f // Si el clic está desactivado, muestra todo normal
        } else {
            holder.itemView.alpha = if (categoriaSeleccionada == nombreCategoria) 1f else 0.5f
        }

        // Configura el evento de clic solo si está habilitado
        if (clicHabilitado) {
            holder.itemView.setOnClickListener {
                categoriaSeleccionada = nombreCategoria
                notifyDataSetChanged()
                onCategoriaClick(nombreCategoria)
            }
        } else {
            holder.itemView.setOnClickListener(null) // Desactiva el clic si no está permitido
        }
    }

    // Devuelve la cantidad total de categorías
    override fun getItemCount(): Int = categorias.size
}