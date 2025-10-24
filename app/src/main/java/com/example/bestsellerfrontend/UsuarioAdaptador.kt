package com.example.bestsellerfrontend

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UsuarioAdaptador(
    private var listaUsuarios: MutableList<Usuario>,
    private val onEliminarClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdaptador.UsuarioViewHolder>() {

    // --- ViewHolder ---
    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgUsuario: ImageView = itemView.findViewById(R.id.imgUsuario)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvCorreo: TextView = itemView.findViewById(R.id.tvCorreo)
        val tvCiudad: TextView = itemView.findViewById(R.id.tvCiudad)
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }

    // --- Inflar la vista ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_usuario, parent, false)
        Log.d("RecyclerDebug", "onCreateViewHolder llamado")
        return UsuarioViewHolder(vista)
    }

    // --- Asignar datos a cada usuario ---
    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaUsuarios[position]

        Log.d("RecyclerDebug", "Mostrando usuario: ${usuario.nombre}")

        // 🔹 Asegurarnos de que los TextViews tengan contenido
        holder.tvNombre.text = usuario.nombre.ifEmpty { "Usuario sin nombre" }
        holder.tvCorreo.text = usuario.correo.ifEmpty { "Correo no disponible" }
        holder.tvCiudad.text = "Ciudad: ${usuario.ciudad.ifEmpty { "No especificada" }}"
        holder.tvId.text = "ID: ${usuario.id ?: "N/D"}"

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(usuario.urlImagen ?: R.drawable.perfil)
            .placeholder(R.drawable.perfil)
            .error(R.drawable.perfil)
            .circleCrop()
            .into(holder.imgUsuario)

        // Acción eliminar
        holder.btnEliminar.setOnClickListener {
            onEliminarClick(usuario)
        }
    }

    override fun getItemCount(): Int {
        Log.d("RecyclerDebug", "getItemCount: ${listaUsuarios.size}")
        return listaUsuarios.size
    }

    // --- Permitir actualizar la lista dinámicamente ---
    fun actualizarLista(nuevaLista: List<Usuario>) {
        listaUsuarios.clear()
        listaUsuarios.addAll(nuevaLista)
        Log.d("RecyclerDebug", "Lista actualizada: ${listaUsuarios.size} usuarios")
        notifyDataSetChanged()
    }
}