package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificacionAdaptador(private val lista: List<Notificacion>) :
    RecyclerView.Adapter<NotificacionAdaptador.NotificacionViewHolder>() {

    class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgUsuario: ImageView = itemView.findViewById(R.id.imgUsuario)
        val txtUsuario: TextView = itemView.findViewById(R.id.txtUsuario)
        val txtMensaje: TextView = itemView.findViewById(R.id.txtMensaje)
        val txtTiempo: TextView = itemView.findViewById(R.id.txtTiempo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val notificacion = lista[position]
        holder.txtUsuario.text = "${notificacion.usuario} hizo una publicación"
        holder.txtMensaje.text = notificacion.mensaje
        holder.txtTiempo.text = notificacion.tiempo

        // Imagen estática por ahora (puedes cambiarla a dinámico si quieres)
        holder.imgUsuario.setImageResource(R.drawable.ic_persona)
    }

    override fun getItemCount(): Int = lista.size
}
