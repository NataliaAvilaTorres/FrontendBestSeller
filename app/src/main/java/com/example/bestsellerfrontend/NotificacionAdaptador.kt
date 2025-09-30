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
        holder.txtUsuario.text = "${notificacion.usuario} publicó una oferta"
        holder.txtMensaje.text = notificacion.mensaje
        holder.txtTiempo.text = tiempoRelativo(notificacion.timestamp)
        holder.imgUsuario.setImageResource(R.drawable.perfil)
    }


    override fun getItemCount(): Int = lista.size

    private fun tiempoRelativo(timestamp: Long?): String {
        if (timestamp == null) return ""
        val diff = System.currentTimeMillis() - timestamp

        val segundos = diff / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            segundos < 60 -> "Hace ${segundos}s"
            minutos < 60 -> "Hace ${minutos}m"
            horas < 24 -> "Hace ${horas}h"
            else -> "Hace ${dias}d"
        }
    }

}