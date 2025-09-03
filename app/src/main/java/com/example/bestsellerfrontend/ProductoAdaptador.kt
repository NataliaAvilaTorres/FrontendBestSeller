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

        // Evento clic en todo el item
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if (context is AppCompatActivity) {
                val fragment = DetalleProductoFragment()
                fragment.arguments = android.os.Bundle().apply {
                    putString("producto_nombre", producto.nombre)
                    putString("producto_categoria", producto.categoria)
                    putDouble("producto_precio", producto.precio)
                    putString("producto_imagen", producto.urlImagen)
                }

                // Reemplazar el fragmento actual por el detalle
                context.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment) // tu FrameLayout en Actividad_Navegacion_Usuario
                    .addToBackStack(null) // permite regresar con el botón de back
                    .commit()
            }
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    // Metodo para actualizar lista
    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}
