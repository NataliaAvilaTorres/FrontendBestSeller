package com.example.bestsellerfrontend

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OfertaAdaptador(
    private var listaOfertas: List<Oferta>,
    private val context: Context,
    private val apiService: ApiService,
    private val mostrarBotones: Boolean = false
) : RecyclerView.Adapter<OfertaAdaptador.OfertaViewHolder>() {

    // Cache simple de productos por id
    private val productosCache: MutableMap<String, Producto> = mutableMapOf()
    private var productosCargados = false

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

        // NUEVOS
        val precioOriginal: TextView = itemView.findViewById(R.id.precioOriginal)
        val precioNuevo: TextView = itemView.findViewById(R.id.precioNuevo) // NOTE: typo guard — see fix below
        val fechaHasta: TextView = itemView.findViewById(R.id.fechaHasta)
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

        val formatoCabecera = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.textFecha.text = formatoCabecera.format(Date(oferta.fechaOferta))

        // ------- Fecha HASTA (debajo del precio nuevo) -------
        val dfHasta = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.fechaHasta.text = "Hasta ${dfHasta.format(Date(oferta.fechaFinal))}"

        // ------- Precio ORIGINAL tachado + PRECIO NUEVO -------
        // Preparar tachado
        holder.precioOriginal.paintFlags =
            holder.precioOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        val productoId = oferta.productoId
        if (!productoId.isNullOrEmpty()) {
            val cacheado = productosCache[productoId]
            if (cacheado != null) {
                bindPrecios(holder, cacheado)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (!productosCargados) {
                            val productos = apiService.listarProductos()
                            productos.forEach { p -> p.id?.let { productosCache[it] = p } }
                            productosCargados = true
                        }
                        val producto = productosCache[productoId]
                        launch(Dispatchers.Main) {
                            if (producto != null) {
                                bindPrecios(holder, producto)
                            } else {
                                holder.precioOriginal.visibility = View.GONE
                                holder.precioNuevo.visibility = View.GONE
                            }
                        }
                    } catch (_: Exception) {
                        launch(Dispatchers.Main) {
                            holder.precioOriginal.visibility = View.GONE
                            holder.precioNuevo.visibility = View.GONE
                        }
                    }
                }
            }
        } else {
            holder.precioOriginal.visibility = View.GONE
            holder.precioNuevo.visibility = View.GONE
        }

        // ------- Usuario (Firebase) -------
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

        // ------- Likes -------
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

        // ------- Ubicación → Mapa -------
        holder.textUbicacion.setOnClickListener {
            val ubicacion = oferta.ubicacion
            if (ubicacion != null) {
                // Crear el fragmento del mapa
                val mapaFragment = MapaFragment().apply {
                    arguments = Bundle().apply {
                        putDouble("destino_lat", ubicacion.lat)
                        putDouble("destino_lng", ubicacion.lng)
                        putString("destino_direccion", ubicacion.direccion ?: "Ubicación de la tienda")
                    }
                }

                // Reemplazar el fragmento dentro del contenedor principal
                val activity = context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, mapaFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Esta oferta no tiene ubicación registrada", Toast.LENGTH_SHORT).show()
            }
        }

        // ------- Imagen del producto (Firebase) -------
        if (!oferta.productoId.isNullOrEmpty()) {
            val refProductos = FirebaseDatabase.getInstance().getReference("productos")
            refProductos.get().addOnSuccessListener { snapshot ->
                var urlImagen: String? = null
                for (tiendaSnapshot in snapshot.children) {
                    val productoSnapshot = tiendaSnapshot.child(oferta.productoId!!)
                    if (productoSnapshot.exists()) {
                        urlImagen = productoSnapshot.child("urlImagen").getValue(String::class.java)
                        break
                    }
                }
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
        }

        // ------- Botones editar/eliminar -------
        if (mostrarBotones) {
            holder.btnEditar?.visibility = View.VISIBLE
            holder.btnEliminar?.visibility = View.VISIBLE
            holder.btnEditar?.setOnClickListener {
                val fragment = FormularioNuevaOfertaFragment().apply {
                    arguments = Bundle().apply { putSerializable("oferta", oferta) }
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
    }

    private fun bindPrecios(holder: OfertaViewHolder, producto: Producto) {
        val nf = NumberFormat.getInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }

        // Precio original (tachado)
        holder.precioOriginal.visibility = View.VISIBLE
        holder.precioOriginal.paintFlags =
            holder.precioOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        holder.precioOriginal.text = "$ ${nf.format(producto.precio)}"

        // Precio nuevo si llega
        val nuevo = producto.precioHasta
        if (nuevo != null) {
            holder.precioNuevo.visibility = View.VISIBLE
            holder.precioNuevo.text = "$ ${nf.format(nuevo)}"
        } else {
            holder.precioNuevo.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}
