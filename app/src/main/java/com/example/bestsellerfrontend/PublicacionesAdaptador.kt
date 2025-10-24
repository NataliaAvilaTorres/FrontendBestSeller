package com.example.bestsellerfrontend

import android.app.AlertDialog
import android.content.Context
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
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PublicacionesAdaptador(
    private var listaOfertas: List<Oferta>,
    private val context: Context,
    private val apiService: ApiService,
    private val mostrarBotones: Boolean = true
) : RecyclerView.Adapter<PublicacionesAdaptador.OfertaViewHolder>() {

    private val productosCache: MutableMap<String, Producto> = mutableMapOf()
    private val tiendasCache: MutableMap<String, Tienda> = mutableMapOf()
    private var productosCargados = false
    private var tiendasCargadas = false

    class OfertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView? = itemView.findViewById(R.id.profileImage)
        val userName: TextView? = itemView.findViewById(R.id.userName)
        val fechaOferta: TextView? = itemView.findViewById(R.id.fechaOferta)
        val imagenProducto: ImageView? = itemView.findViewById(R.id.imagenOferta)
        val nombreOferta: TextView? = itemView.findViewById(R.id.nombreOferta)
        val descripcionOferta: TextView? = itemView.findViewById(R.id.descripcionOferta)
        val imagenTienda: ImageView? = itemView.findViewById(R.id.imagenTienda)
        val nombreTienda: TextView? = itemView.findViewById(R.id.nombreTienda)
        val precioNuevo: TextView? = itemView.findViewById(R.id.precioNuevo)
        val fechaHasta: TextView? = itemView.findViewById(R.id.fechaHasta)
        val btnLike: ImageButton? = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView? = itemView.findViewById(R.id.tvLikeCount)
        val btnEliminar: Button? = itemView.findViewById(R.id.btnEliminar)
        // Opcionales que no están en tu layout
        val btnEditar: Button? = itemView.findViewById(R.id.btnEditar)
        val textUbicacion: TextView? = itemView.findViewById(R.id.ubicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_oferta2, parent, false)
        return OfertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        // Nombre y descripción
        holder.nombreOferta?.text = "🎉 ${oferta.nombreOferta}"
        holder.descripcionOferta?.text = oferta.descripcionOferta

        // Fecha de creación
        val formatoCabecera = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.fechaOferta?.text = formatoCabecera.format(Date(oferta.fechaOferta))

        // Fecha hasta
        val dfHasta = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.fechaHasta?.text = "Hasta ${dfHasta.format(Date(oferta.fechaFinal))}"

        // Precio nuevo
        val productoId = oferta.productoId
        if (!productoId.isNullOrEmpty()) {
            val cacheado = productosCache[productoId]
            if (cacheado != null) {
                bindPrecioNuevo(holder, cacheado)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (!productosCargados) {
                            val productos = apiService.listarProductos()
                            productos.forEach { p -> p.id?.let { productosCache[it] = p } }
                            productosCargados = true
                        }
                        val prod = productosCache[productoId]
                        withContext(Dispatchers.Main) {
                            if (prod != null) bindPrecioNuevo(holder, prod)
                            else holder.precioNuevo?.text = "$ 0"
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) { holder.precioNuevo?.text = "$ 0" }
                    }
                }
            }
        } else {
            holder.precioNuevo?.text = "$ 0"
        }

        // Tienda
        val tiendaId = oferta.tiendaId
        val tiendaCache = tiendasCache[tiendaId]
        if (tiendaCache != null) {
            bindTienda(holder, tiendaCache)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!tiendasCargadas) {
                        val tiendas = apiService.listarTiendas()
                        tiendas.forEach { t -> tiendasCache[t.id] = t }
                        tiendasCargadas = true
                    }
                    val tienda = tiendasCache[tiendaId]
                    withContext(Dispatchers.Main) {
                        if (tienda != null) bindTienda(holder, tienda)
                        else {
                            holder.nombreTienda?.text = "Tienda desconocida"
                            holder.imagenTienda?.setImageResource(R.drawable.fondo_imagen_redonda)
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        holder.nombreTienda?.text = "Tienda desconocida"
                        holder.imagenTienda?.setImageResource(R.drawable.fondo_imagen_redonda)
                    }
                }
            }
        }

        // Likes
        val prefs = context.getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioIdSesion = prefs.getString("id", null)
        val yaDioLike = usuarioIdSesion != null && (oferta.likedBy[usuarioIdSesion] == true)
        holder.tvLikeCount?.text = oferta.likes.toString()
        holder.btnLike?.setColorFilter(
            if (yaDioLike) ContextCompat.getColor(context, R.color.rojo)
            else ContextCompat.getColor(context, R.color.black)
        )

        holder.btnLike?.setOnClickListener {
            if (usuarioIdSesion == null) {
                Toast.makeText(context, "Debes iniciar sesión para dar like", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val nuevoEstado = !(oferta.likedBy[usuarioIdSesion] ?: false)
            oferta.likedBy =
                oferta.likedBy.toMutableMap().apply { put(usuarioIdSesion, nuevoEstado) }
            oferta.likes = if (nuevoEstado) oferta.likes + 1 else maxOf(0, oferta.likes - 1)
            holder.tvLikeCount?.text = oferta.likes.toString()
            holder.btnLike?.setColorFilter(
                if (nuevoEstado) ContextCompat.getColor(context, R.color.rojo)
                else ContextCompat.getColor(context, R.color.black)
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    apiService.toggleLike(oferta.id, usuarioIdSesion, nuevoEstado)
                } catch (_: Exception) {}
            }
        }

        // Botón eliminar
        if (mostrarBotones) {
            holder.btnEliminar?.visibility = View.VISIBLE
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
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } else {
            holder.btnEliminar?.visibility = View.GONE
        }
    }

    private fun bindPrecioNuevo(holder: OfertaViewHolder, producto: Producto) {
        val nf = NumberFormat.getInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }
        val nuevo = producto.precioHasta ?: producto.precio
        holder.precioNuevo?.text = "$ ${nf.format(nuevo)}"
    }

    private fun bindTienda(holder: OfertaViewHolder, tienda: Tienda) {
        holder.nombreTienda?.text = tienda.nombre
        Glide.with(holder.itemView.context)
            .load(tienda.urlImagen)
            .placeholder(R.drawable.fondo_imagen_redonda)
            .error(R.drawable.fondo_imagen_redonda)
            .circleCrop()
            .into(holder.imagenTienda!!)
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}