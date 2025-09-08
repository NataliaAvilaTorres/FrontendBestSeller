package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class CategoriaAdaptador(
    private val categorias: List<Pair<Int, String>>
) : RecyclerView.Adapter<CategoriaAdaptador.CategoriaViewHolder>() {

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
        val (iconRes, nombre) = categorias[position]
        holder.icono.setImageResource(iconRes)
        holder.nombre.text = nombre
    }

    override fun getItemCount(): Int = categorias.size
}
