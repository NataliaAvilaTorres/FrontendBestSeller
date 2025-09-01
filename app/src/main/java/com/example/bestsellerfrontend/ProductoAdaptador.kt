package com.example.bestsellerfrontend

import android.content.Intent
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

        // Agregar evento clic en todo el item
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Actividad_vista_detalles_producto::class.java)

            // Enviar datos del producto (puedes usar Parcelable o Serializable)
            intent.putExtra("producto_nombre", producto.nombre)
            intent.putExtra("producto_categoria", producto.categoria)
            intent.putExtra("producto_precio", producto.precio)
            intent.putExtra("producto_imagen", producto.urlImagen)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    // Metodo para actualizar lista
    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}
