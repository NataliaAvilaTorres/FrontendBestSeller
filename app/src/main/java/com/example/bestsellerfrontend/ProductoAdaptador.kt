package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductoAdaptador(private var listaProductos: List<Producto>) :
    RecyclerView.Adapter<ProductoAdaptador.ProductoViewHolder>() {

    // ViewHolder: representa un item
    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProducto: ImageView = itemView.findViewById(R.id.imagenProducto)
        val textNombre: TextView = itemView.findViewById(R.id.nombreProduct)
        val textCategoria: TextView = itemView.findViewById(R.id.categoriaProducto)
        val textPrecio: TextView = itemView.findViewById(R.id.product_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        // Asignar datos al layout
        holder.textNombre.text = producto.nombre
        holder.textCategoria.text = producto.categoria
        holder.textPrecio.text = "$${producto.precio}"

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(producto.urlImagen)
            .into(holder.imageProducto)
    }

    override fun getItemCount(): Int = listaProductos.size

    // Método para actualizar lista
    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}