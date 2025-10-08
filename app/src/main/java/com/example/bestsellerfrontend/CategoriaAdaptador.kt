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
    private val clicHabilitado: Boolean = true // 🔹 parámetro para activar o desactivar clics
) : RecyclerView.Adapter<CategoriaAdaptador.CategoriaViewHolder>() {

    private var categoriaSeleccionada: String? = null

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icono: ImageView = itemView.findViewById(R.id.iconoCategoria)
        val nombre: TextView = itemView.findViewById(R.id.nombreCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val (iconRes, nombreCategoria) = categorias[position]
        holder.icono.setImageResource(iconRes)
        holder.nombre.text = nombreCategoria

        if (!clicHabilitado) {
            holder.itemView.alpha = 1f
        } else {
            holder.itemView.alpha = if (categoriaSeleccionada == nombreCategoria) 1f else 0.5f
        }

        if (clicHabilitado) {
            holder.itemView.setOnClickListener {
                categoriaSeleccionada = nombreCategoria
                notifyDataSetChanged()
                onCategoriaClick(nombreCategoria)
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = categorias.size
}