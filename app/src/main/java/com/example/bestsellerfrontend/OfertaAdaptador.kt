package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class OfertaAdaptador(private var listaOfertas: List<Oferta>) :
    RecyclerView.Adapter<OfertaAdaptador.OfertaViewHolder>() {

    class OfertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagenOferta: ImageView = itemView.findViewById(R.id.imagenOferta)
        val textNombre: TextView = itemView.findViewById(R.id.nombreOferta)
        val textDescripcion: TextView = itemView.findViewById(R.id.descripcionOferta)
        val textFecha: TextView = itemView.findViewById(R.id.fechaOferta)
        val textUbicacion: TextView = itemView.findViewById(R.id.ubicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_oferta, parent, false)
        return OfertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        holder.textNombre.text = oferta.nombreOferta
        holder.textDescripcion.text = oferta.descripcionOferta

        val fecha = Date(oferta.fechaOferta)
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaFormateada = formato.format(fecha)

        holder.textFecha.text = fechaFormateada

        Glide.with(holder.itemView.context)
            .load(oferta.urlImagen)
            .into(holder.imagenOferta)
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}