package com.example.bestsellerfrontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ProductoAdaptador(
    private var listaProductos: List<Producto>,
    private val listaTiendas: List<Tienda>
) : RecyclerView.Adapter<ProductoAdaptador.ProductoViewHolder>() {


    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProducto: ImageView = itemView.findViewById(R.id.imagenProducto)
        val textNombre: TextView = itemView.findViewById(R.id.nombreProduct)
        val textCategoria: TextView = itemView.findViewById(R.id.categoriaProducto)
        val textPrecio: TextView = itemView.findViewById(R.id.product_price)
        val imageTienda: ImageView = itemView.findViewById(R.id.imagenTienda)
        val textTienda: TextView = itemView.findViewById(R.id.nombreTienda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.actividad_vista_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        // Buscar la tienda correspondiente al producto
        val tienda = listaTiendas.find { it.id == producto.tiendaId }

        // Asignar textos a los elementos visuales
        holder.textNombre.text = producto.nombre
        holder.textCategoria.text = producto.marca.categoria
        holder.textPrecio.text = "$${producto.precio}"
        holder.textTienda.text = tienda?.nombre ?: "Tienda desconocida"

        // Cargar imagen del producto con Glide
        Glide.with(holder.itemView.context)
            .load(producto.urlImagen)
            .placeholder(R.drawable.producto)
            .error(R.drawable.producto)
            .into(holder.imageProducto)

        // Cargar imagen de la tienda (en formato circular)
        Glide.with(holder.itemView.context)
            .load(tienda?.urlImagen)
            .placeholder(R.drawable.fondo_imagen_redonda)
            .error(R.drawable.fondo_imagen_redonda)
            .circleCrop()
            .into(holder.imageTienda)

        // Acción al hacer clic en un producto → abrir fragmento de detalle
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if (context is AppCompatActivity) {
                val fragment = DetalleProductoFragment()
                fragment.arguments = android.os.Bundle().apply {
                    putString("producto_nombre", producto.nombre)
                    putString("producto_categoria", producto.marca.categoria)
                    putDouble("producto_precio", producto.precio)
                    putString("producto_imagen", producto.urlImagen)
                    putString("tienda_nombre", tienda?.nombre ?: "")
                    putString("tienda_imagen", tienda?.urlImagen ?: "")
                }

                // Reemplazar el fragmento actual por el de detalle del producto
                context.supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    /**
     * Retorna la cantidad de elementos (productos) que mostrará el RecyclerView.
     */
    override fun getItemCount(): Int = listaProductos.size

    /**
     * Permite actualizar la lista de productos y refrescar la vista del RecyclerView.
     */
    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}