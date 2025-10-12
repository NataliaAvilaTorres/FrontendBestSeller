package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductoAdaptador(private var listaProductos: List<Producto>) :
    RecyclerView.Adapter<ProductoAdaptador.ProductoViewHolder>() {

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

        holder.textNombre.text = producto.nombre
        holder.textCategoria.text = producto.marca?.categoria ?: "Sin categoría"
        holder.textPrecio.text = "$${producto.precio}"

        // Cargar imagen del producto o por defecto si no existe
        val imagenUrl = producto.urlImagen
        Glide.with(holder.itemView.context)
            .load(if (!imagenUrl.isNullOrEmpty()) imagenUrl else R.drawable.producto)
            .placeholder(R.drawable.producto)
            .error(R.drawable.producto)
            .into(holder.imageProducto)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if (context is AppCompatActivity) {
                val fragment = DetalleProductoFragment()
                fragment.arguments = android.os.Bundle().apply {
                    putString("producto_nombre", producto.nombre)
                    putString("producto_categoria", producto.marca?.categoria ?: "")
                    putDouble("producto_precio", producto.precio)
                    putString("producto_imagen", producto.urlImagen)
                }

                context.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}