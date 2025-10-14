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

    // Se ejecuta cuando se crea una nueva vista para un elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    // Se ejecuta para asignar los datos a cada vista según su posición
    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        // Obtener la notificación actual según la posición
        val notificacion = lista[position]

        // Mostrar el nombre del usuario y el texto del mensaje
        holder.txtUsuario.text = "${notificacion.usuario} publicó una oferta"
        holder.txtMensaje.text = notificacion.mensaje

        // Mostrar el tiempo relativo desde que se generó la notificación
        holder.txtTiempo.text = tiempoRelativo(notificacion.timestamp)

        // Asignar una imagen de perfil por defecto
        holder.imgUsuario.setImageResource(R.drawable.perfil)
    }

    // Retorna la cantidad total de elementos en la lista
    override fun getItemCount(): Int = lista.size

    // Función auxiliar para calcular el tiempo relativo (segundos, minutos, horas o días)
    private fun tiempoRelativo(timestamp: Long?): String {
        if (timestamp == null) return ""

        // Calcular diferencia de tiempo entre ahora y el timestamp
        val diff = System.currentTimeMillis() - timestamp

        // Convertir a unidades de tiempo
        val segundos = diff / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        // Retornar el texto según la cantidad de tiempo transcurrido
        return when {
            segundos < 60 -> "Hace ${segundos}s"
            minutos < 60 -> "Hace ${minutos}m"
            horas < 24 -> "Hace ${horas}h"
            else -> "Hace ${dias}d"
        }
    }
}