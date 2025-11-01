package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopOfertasAdapter(private var ofertas: List<Oferta>) :
    RecyclerView.Adapter<TopOfertasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreOferta)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        val tvPosicion: TextView = view.findViewById(R.id.tvPosicion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_oferta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val oferta = ofertas[position]

        holder.tvPosicion.text = "${position + 1}"
        holder.tvNombre.text = oferta.nombreOferta
        holder.tvLikes.text = "❤️ ${oferta.likes} likes"

        // Medalla para top 3
        when (position) {
            0 -> holder.tvPosicion.text = "🥇"
            1 -> holder.tvPosicion.text = "🥈"
            2 -> holder.tvPosicion.text = "🥉"
        }
    }

    override fun getItemCount() = ofertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        ofertas = nuevaLista
        notifyDataSetChanged()
    }
}