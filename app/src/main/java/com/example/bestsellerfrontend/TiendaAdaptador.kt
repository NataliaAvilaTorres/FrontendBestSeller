package com.example.bestsellerfrontend

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TiendaAdaptador(
    private var listaTiendas: List<Tienda>,
    private val context: Context,
    private val onTiendaClick: ((Tienda) -> Unit)? = null
) : RecyclerView.Adapter<TiendaAdaptador.TiendaViewHolder>() {

    class TiendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreTienda: TextView = itemView.findViewById(R.id.nombreTienda)
        val imagenTienda: ImageView = itemView.findViewById(R.id.imagenTienda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_tienda, parent, false)
        return TiendaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int) {
        val tienda = listaTiendas[position]

        holder.nombreTienda.text = tienda.nombre

        Glide.with(context)
            .load(tienda.urlImagen)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imagenTienda)

        holder.itemView.setOnClickListener {
            onTiendaClick?.invoke(tienda)
        }
    }

    override fun getItemCount(): Int = listaTiendas.size

    fun actualizarLista(nuevaLista: List<Tienda>) {
        listaTiendas = nuevaLista
        notifyDataSetChanged()
    }
}
