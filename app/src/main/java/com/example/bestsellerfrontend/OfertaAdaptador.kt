package com.example.bestsellerfrontend

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OfertaAdaptador(
    private var listaOfertas: List<Oferta>,
    private val context: Context,
    private val apiService: ApiService
) : RecyclerView.Adapter<OfertaAdaptador.OfertaViewHolder>() {

    class OfertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagenOferta: ImageView = itemView.findViewById(R.id.imagenOferta)
        val textNombre: TextView = itemView.findViewById(R.id.nombreOferta)
        val textDescripcion: TextView = itemView.findViewById(R.id.descripcionOferta)
        val textFecha: TextView = itemView.findViewById(R.id.fechaOferta)
        val textUbicacion: TextView = itemView.findViewById(R.id.ubicacion)
        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_oferta, parent, false)
        return OfertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        // Mostrar nombre, descripción y fecha
        holder.textNombre.text = "🎉 ${oferta.nombreOferta}"
        holder.textDescripcion.text = oferta.descripcionOferta
        val fecha = Date(oferta.fechaOferta)
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.textFecha.text = formato.format(fecha)

        // Imagen con Glide
        Glide.with(holder.itemView.context)
            .load(oferta.urlImagen)
            .into(holder.imagenOferta)

        // Inicializar contador de likes (asegúrate de agregar estas propiedades en Oferta.kt)
        if (oferta.likes == null) oferta.likes = 0
        if (oferta.likedByUser == null) oferta.likedByUser = false
        holder.tvLikeCount.text = oferta.likes.toString()

        // Cambiar color del botón según el estado
        holder.btnLike.setColorFilter(
            if (oferta.likedByUser) ContextCompat.getColor(context, R.color.rojo)
            else ContextCompat.getColor(context, R.color.black)
        )

        // Listener para toggle
        holder.btnLike.setOnClickListener {
            val nuevoEstado = !oferta.likedByUser
            oferta.likedByUser = nuevoEstado

            if (nuevoEstado) {
                oferta.likes += 1
                holder.btnLike.setColorFilter(ContextCompat.getColor(context, R.color.rojo))
            } else {
                oferta.likes -= 1
                holder.btnLike.setColorFilter(ContextCompat.getColor(context, R.color.black))
            }

            holder.tvLikeCount.text = oferta.likes.toString()

            // Llamada al backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Asegúrate de que la oferta tenga su id de Firebase
                    apiService.toggleLike(oferta.id ?: "", nuevoEstado)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}
