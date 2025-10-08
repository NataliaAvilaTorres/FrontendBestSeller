package com.example.bestsellerfrontend

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OfertaAdaptador(
    private var listaOfertas: List<Oferta>,
    private val context: Context,
    private val apiService: ApiService,
    private val mostrarBotones: Boolean = false
) : RecyclerView.Adapter<OfertaAdaptador.OfertaViewHolder>() {

    class OfertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNombre: TextView = itemView.findViewById(R.id.nombreOferta)
        val textDescripcion: TextView = itemView.findViewById(R.id.descripcionOferta)
        val textFecha: TextView = itemView.findViewById(R.id.fechaOferta)
        val textUbicacion: TextView = itemView.findViewById(R.id.ubicacion)
        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val btnEditar: Button? = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button? = itemView.findViewById(R.id.btnEliminar)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val userName: TextView = itemView.findViewById(R.id.userName)
        val imagenProducto: ImageView = itemView.findViewById(R.id.imagenOferta)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_oferta, parent, false)
        return OfertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        holder.textNombre.text = "🎉 ${oferta.nombreOferta}"
        holder.textDescripcion.text = oferta.descripcionOferta

        val fecha = Date(oferta.fechaOferta)
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.textFecha.text = formato.format(fecha)

        if (!oferta.usuarioId.isNullOrEmpty()) {
            val ref = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(oferta.usuarioId!!)

            ref.get().addOnSuccessListener { snapshot ->
                val nombre = snapshot.child("nombre").getValue(String::class.java)
                val foto = snapshot.child("urlImagen").getValue(String::class.java)

                holder.userName.text = nombre ?: "Usuario desconocido"

                if (!foto.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(foto)
                        .placeholder(R.drawable.perfil)
                        .error(R.drawable.perfil)
                        .circleCrop()
                        .into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.perfil)
                }
            }.addOnFailureListener {
                holder.userName.text = "Usuario desconocido"
                holder.profileImage.setImageResource(R.drawable.perfil)
            }
        } else {
            holder.userName.text = "Usuario desconocido"
            holder.profileImage.setImageResource(R.drawable.perfil)
        }

        val prefs = context.getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioIdSesion = prefs.getString("id", null)

        val yaDioLike = usuarioIdSesion != null && (oferta.likedBy[usuarioIdSesion] == true)
        holder.tvLikeCount.text = oferta.likes.toString()

        holder.btnLike.setColorFilter(
            if (yaDioLike) ContextCompat.getColor(context, R.color.rojo)
            else ContextCompat.getColor(context, R.color.black)
        )

        holder.btnLike.setOnClickListener {
            if (usuarioIdSesion == null) {
                Toast.makeText(context, "Debes iniciar sesión para dar like", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoEstado = !(oferta.likedBy[usuarioIdSesion] ?: false)
            oferta.likedBy = oferta.likedBy.toMutableMap().apply { put(usuarioIdSesion, nuevoEstado) }
            oferta.likes = if (nuevoEstado) oferta.likes + 1 else maxOf(0, oferta.likes - 1)

            holder.tvLikeCount.text = oferta.likes.toString()
            holder.btnLike.setColorFilter(
                if (nuevoEstado) ContextCompat.getColor(context, R.color.rojo)
                else ContextCompat.getColor(context, R.color.black)
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    apiService.toggleLike(oferta.id, usuarioIdSesion, nuevoEstado)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        if (mostrarBotones) {
            holder.btnEditar?.visibility = View.VISIBLE
            holder.btnEliminar?.visibility = View.VISIBLE

            holder.btnEditar?.setOnClickListener {
                val fragment = FormularioNuevaOfertaFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("oferta", oferta)
                    }
                }
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            holder.btnEliminar?.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Eliminar oferta")
                    .setMessage("¿Seguro que quieres eliminar esta oferta?")
                    .setPositiveButton("Sí") { _, _ ->
                        (context as AppCompatActivity).lifecycleScope.launch {
                            try {
                                val respuesta = apiService.eliminarOferta(oferta.id)
                                Toast.makeText(context, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                                listaOfertas = listaOfertas.filter { it.id != oferta.id }
                                notifyDataSetChanged()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } else {
            holder.btnEditar?.visibility = View.GONE
            holder.btnEliminar?.visibility = View.GONE
        }


        holder.textUbicacion.setOnClickListener {
            val ubicacion = oferta.ubicacion
            if (ubicacion != null) {
                val intent = Intent(context, Actividad_Mapa::class.java).apply {
                    putExtra("destino_lat", ubicacion.lat)
                    putExtra("destino_lng", ubicacion.lng)
                    putExtra("destino_direccion", ubicacion.direccion ?: "Ubicación de la tienda")
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Esta oferta no tiene ubicación registrada", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Cargar imagen del producto ---
        if (!oferta.productoId.isNullOrEmpty()) {
            val refProducto = FirebaseDatabase.getInstance()
                .getReference("productos")
                .child(oferta.productoId!!)

            refProducto.get().addOnSuccessListener { snapshot ->
                val urlImagen = snapshot.child("urlImagen").getValue(String::class.java)
                if (!urlImagen.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(urlImagen)
                        .placeholder(R.drawable.producto)
                        .error(R.drawable.producto)
                        .into(holder.imagenProducto)
                } else {
                    holder.imagenProducto.setImageResource(R.drawable.producto)
                }
            }.addOnFailureListener {
                holder.imagenProducto.setImageResource(R.drawable.producto)
            }
        } else {
            holder.imagenProducto.setImageResource(R.drawable.producto)
        }

    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}
