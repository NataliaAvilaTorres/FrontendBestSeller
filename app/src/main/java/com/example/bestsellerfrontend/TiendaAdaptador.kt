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
    private val layoutId: Int, // Layout a inflar
    private val onTiendaClick: ((Tienda) -> Unit)? = null
) : RecyclerView.Adapter<TiendaAdaptador.TiendaViewHolder>() {

    // ViewHolder para cada tienda
    class TiendaViewHolder(itemView: View, layoutId: Int) : RecyclerView.ViewHolder(itemView) {
        val nombreTienda: TextView = itemView.findViewById(R.id.nombreTienda)
        val imagenTienda: ImageView = if (layoutId == R.layout.item_tienda)
            itemView.findViewById(R.id.logoTienda)
        else
            itemView.findViewById(R.id.imagenTienda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return TiendaViewHolder(view, layoutId)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int) {
        val tienda = listaTiendas[position]
        holder.nombreTienda.text = tienda.nombre

        // Cargar imagen con Glide
        Glide.with(context)
            .load(tienda.urlImagen)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imagenTienda)

        // Click en tienda para filtrar productos
        holder.itemView.setOnClickListener { onTiendaClick?.invoke(tienda) }
    }

    override fun getItemCount(): Int = listaTiendas.size

    // Actualiza la lista de tiendas y refresca el RecyclerView
    fun actualizarLista(nuevaLista: List<Tienda>) {
        listaTiendas = nuevaLista
        notifyDataSetChanged()
    }
}