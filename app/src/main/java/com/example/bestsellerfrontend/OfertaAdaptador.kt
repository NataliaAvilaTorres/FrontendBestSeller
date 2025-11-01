package com.example.bestsellerfrontend

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OfertaAdaptador(
    private var listaOfertas: List<Oferta>,
    private val context: Context,
    private val apiService: ApiService,
    private val mostrarBotones: Boolean = false
) : RecyclerView.Adapter<OfertaAdaptador.OfertaViewHolder>() {

    private val productosCache: MutableMap<String, Producto> = mutableMapOf()
    private val tiendasCache: MutableMap<String, Tienda> = mutableMapOf()
    private var productosCargados = false
    private var tiendasCargados = false

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
        val fechaHasta: TextView = itemView.findViewById(R.id.fechaHasta)
        val precioNuevo: TextView = itemView.findViewById(R.id.precioNuevo)
        val imagenTienda: ImageView = itemView.findViewById(R.id.imagenTienda)
        val nombreTienda: TextView = itemView.findViewById(R.id.nombreTienda)
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

        val dfHasta = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.fechaHasta.text = "Hasta ${dfHasta.format(Date(oferta.fechaFinal))}"

        // ====== PRECIO NUEVO (desde Firebase) ======
        if (!oferta.productoId.isNullOrEmpty() && !oferta.tiendaId.isNullOrEmpty()) {
            val refProducto = FirebaseDatabase.getInstance().getReference("productos")
                .child(oferta.tiendaId)
                .child(oferta.productoId!!)

            refProducto.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val precioHasta = snapshot.child("precioHasta").getValue(Double::class.java)
                    val precio = snapshot.child("precio").getValue(Double::class.java)

                    val nf = NumberFormat.getInstance(Locale.getDefault()).apply {
                        maximumFractionDigits = 2
                    }

                    val precioMostrar = precioHasta ?: precio ?: 0.0
                    holder.precioNuevo.text = "💰 \$${nf.format(precioMostrar)}"
                } else {
                    holder.precioNuevo.text = "$ 0"
                }
            }.addOnFailureListener {
                holder.precioNuevo.text = "$ 0"
            }
        } else {
            holder.precioNuevo.text = "$ 0"
        }

        // ====== TIENDA (imagen circular + nombre) ======
        val tiendaId = oferta.tiendaId
        val tiendaCache = tiendasCache[tiendaId]
        if (tiendaCache != null) {
            bindTienda(holder, tiendaCache)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!tiendasCargados) {
                        val tiendas = apiService.listarTiendas()
                        tiendas.forEach { t -> tiendasCache[t.id] = t }
                        tiendasCargados = true
                    }
                    val tienda = tiendasCache[tiendaId]
                    withContext(Dispatchers.Main) {
                        if (tienda != null) bindTienda(holder, tienda)
                        else {
                            holder.nombreTienda.text = "Tienda desconocida"
                            holder.imagenTienda.setImageResource(R.drawable.fondo_imagen_redonda)
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        holder.nombreTienda.text = "Tienda desconocida"
                        holder.imagenTienda.setImageResource(R.drawable.fondo_imagen_redonda)
                    }
                }
            }
        }

        // ====== Usuario ======
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
                } else holder.profileImage.setImageResource(R.drawable.perfil)
            }.addOnFailureListener {
                holder.userName.text = "Usuario desconocido"
                holder.profileImage.setImageResource(R.drawable.perfil)
            }
        }

        // ====== Imagen de producto ======
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
                } else holder.imagenProducto.setImageResource(R.drawable.producto)
            }.addOnFailureListener {
                holder.imagenProducto.setImageResource(R.drawable.producto)
            }
        }

        // ====== Likes ======
        val prefs = context.getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioIdSesion = prefs.getString("id", null)
        val yaDioLike = usuarioIdSesion != null && (oferta.likedBy[usuarioIdSesion] == true)
        holder.tvLikeCount.text = oferta.likes.toString()
        holder.btnLike.setColorFilter(
            if (yaDioLike) ContextCompat.getColor(context, android.R.color.holo_red_dark)
            else ContextCompat.getColor(context, android.R.color.black)
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
                if (nuevoEstado) ContextCompat.getColor(context, android.R.color.holo_red_dark)
                else ContextCompat.getColor(context, android.R.color.black)
            )
            CoroutineScope(Dispatchers.IO).launch {
                try { apiService.toggleLike(oferta.id, usuarioIdSesion, nuevoEstado) } catch (_: Exception) {}
            }
        }

        // ====== Ver ubicación → Mapa con destino ======
        holder.textUbicacion.setOnClickListener {
            val ubicacion = oferta.ubicacion
            if (ubicacion != null) {
                val mapaFragment = MapaFragment().apply {
                    arguments = Bundle().apply {
                        putDouble("destino_lat", ubicacion.lat)
                        putDouble("destino_lng", ubicacion.lng)
                        putString("destino_direccion", ubicacion.direccion ?: "Ubicación de la tienda")
                    }
                }
                val activity = context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, mapaFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Esta oferta no tiene ubicación registrada", Toast.LENGTH_SHORT).show()
            }
        }

        // ====== Botones editar/eliminar ======
        if (mostrarBotones) {
            holder.btnEditar?.visibility = View.VISIBLE
            holder.btnEliminar?.visibility = View.VISIBLE

            // ✅ BOTÓN EDITAR - Pasar datos al formulario
            holder.btnEditar?.setOnClickListener {
                val fragment = FormularioNuevaOfertaFragment().apply {
                    arguments = Bundle().apply {
                        putString("modo", "editar")                                           // Modo edición
                        putString("oferta_id", oferta.id)                                     // ID de la oferta
                        putString("oferta_nombre", oferta.nombreOferta)                       // Nombre
                        putString("oferta_descripcion", oferta.descripcionOferta)             // Descripción
                        putString("oferta_tienda_id", oferta.tiendaId)                        // Tienda ID
                        putString("oferta_producto_id", oferta.productoId)                    // Producto ID
                        putLong("oferta_fecha_final", oferta.fechaFinal)                      // Fecha final
                    }
                }
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            // ✅ BOTÓN ELIMINAR
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

    private fun bindTienda(holder: OfertaViewHolder, tienda: Tienda) {
        holder.nombreTienda.text = tienda.nombre
        Glide.with(holder.itemView.context)
            .load(tienda.urlImagen)
            .placeholder(R.drawable.fondo_imagen_redonda)
            .error(R.drawable.fondo_imagen_redonda)
            .circleCrop()
            .into(holder.imagenTienda)
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}