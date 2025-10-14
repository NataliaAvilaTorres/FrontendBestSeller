package com.example.bestsellerfrontend

import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
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
    private var productosCargados = false

    // ViewHolder: representa los elementos de una oferta en el RecyclerView
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
        val precioOriginal: TextView = itemView.findViewById(R.id.precioOriginal)
        val precioNuevo: TextView = itemView.findViewById(R.id.precioNuevo)
        val fechaHasta: TextView = itemView.findViewById(R.id.fechaHasta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        // Inflar el layout del ítem
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_oferta, parent, false)
        return OfertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        // Mostrar información básica
        holder.textNombre.text = "🎉 ${oferta.nombreOferta}"
        holder.textDescripcion.text = oferta.descripcionOferta

        // Mostrar fecha de creación de la oferta
        val formatoCabecera = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.textFecha.text = formatoCabecera.format(Date(oferta.fechaOferta))

        // Mostrar fecha de expiración
        val dfHasta = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.fechaHasta.text = "Hasta ${dfHasta.format(Date(oferta.fechaFinal))}"

        // Mostrar precios (original tachado y nuevo)
        holder.precioOriginal.paintFlags =
            holder.precioOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        // Buscar producto y mostrar sus precios (usando cache o red)
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
                        withContext(Dispatchers.Main) {
                            if (producto != null) bindPrecios(holder, producto)
                            else {
                                holder.precioOriginal.visibility = View.GONE
                                holder.precioNuevo.visibility = View.GONE
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
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

        // Cargar información del usuario desde Firebase
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

        // Sistema de likes
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
                Toast.makeText(context, "Debes iniciar sesión para dar like", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val nuevoEstado = !(oferta.likedBy[usuarioIdSesion] ?: false)
            oferta.likedBy =
                oferta.likedBy.toMutableMap().apply { put(usuarioIdSesion, nuevoEstado) }
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

        // Mostrar ubicación de la oferta en mapa
        holder.textUbicacion.setOnClickListener {
            val ubicacion = oferta.ubicacion
            if (ubicacion != null) {
                val mapaFragment = MapaFragment().apply {
                    arguments = Bundle().apply {
                        putDouble("destino_lat", ubicacion.lat)
                        putDouble("destino_lng", ubicacion.lng)
                        putString(
                            "destino_direccion",
                            ubicacion.direccion ?: "Ubicación de la tienda"
                        )
                    }
                }
                val activity = context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, mapaFragment)
                    .addToBackStack(null)
                    .commit()
            } else Toast.makeText(
                context,
                "Esta oferta no tiene ubicación registrada",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Cargar imagen del producto desde Firebase
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

        // Mostrar botones de edición y eliminación si corresponde
        if (mostrarBotones) {
            holder.btnEditar?.visibility = View.VISIBLE
            holder.btnEliminar?.visibility = View.VISIBLE

            // Botón Editar → abre formulario de edición
            holder.btnEditar?.setOnClickListener {
                val fragment = FormularioNuevaOfertaFragment().apply {
                    arguments = Bundle().apply { putSerializable("oferta", oferta) }
                }
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            // Botón Eliminar → muestra confirmación y borra oferta
            holder.btnEliminar?.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Eliminar oferta")
                    .setMessage("¿Seguro que quieres eliminar esta oferta?")
                    .setPositiveButton("Sí") { _, _ ->
                        (context as AppCompatActivity).lifecycleScope.launch {
                            try {
                                val respuesta = apiService.eliminarOferta(oferta.id)
                                Toast.makeText(context, respuesta.mensaje, Toast.LENGTH_SHORT)
                                    .show()
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
            holder.btnEditar?.visibility = View.GONE
            holder.btnEliminar?.visibility = View.GONE
        }
    }

    // Muestra los precios (original y nuevo) con formato
    private fun bindPrecios(holder: OfertaViewHolder, producto: Producto) {
        val nf = NumberFormat.getInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }
        holder.precioOriginal.visibility = View.VISIBLE
        holder.precioOriginal.paintFlags =
            holder.precioOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        holder.precioOriginal.text = "$ ${nf.format(producto.precio)}"
        val nuevo = producto.precioHasta
        if (nuevo != null) {
            holder.precioNuevo.visibility = View.VISIBLE
            holder.precioNuevo.text = "$ ${nf.format(nuevo)}"
        } else holder.precioNuevo.visibility = View.GONE
    }

    override fun getItemCount(): Int = listaOfertas.size

    // Actualiza la lista de ofertas en el adaptador
    fun actualizarLista(nuevaLista: List<Oferta>) {
        listaOfertas = nuevaLista
        notifyDataSetChanged()
    }
}