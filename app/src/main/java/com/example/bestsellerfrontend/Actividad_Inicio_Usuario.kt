package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class InicioUsuarioFragment : Fragment() {

    private lateinit var recyclerViewOfertas: RecyclerView
    private lateinit var adapterOfertas: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador

    private lateinit var recyclerViewTiendas: RecyclerView
    private lateinit var adapterTiendas: TiendaAdaptador

    private lateinit var progressScroll: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_inicio_usuario, container, false)

        // --- Mostrar saludo personalizado con el nombre del usuario ---
        val txtSaludo: TextView = view.findViewById(R.id.txtSaludo)
        val prefs =
            requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre", "Usuario")
        txtSaludo.text = "Hola $nombreUsuario, Buen día!"

        // --- Configuración del RecyclerView de Ofertas ---
        recyclerViewOfertas = view.findViewById(R.id.recyclerViewOfertas)
        recyclerViewOfertas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // --- Barra de progreso para el desplazamiento horizontal ---
        progressScroll = view.findViewById(R.id.progressScroll)
        recyclerViewOfertas.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Calcula el progreso del scroll y lo muestra en la barra
                val extent = recyclerView.computeHorizontalScrollExtent()
                val range = recyclerView.computeHorizontalScrollRange()
                val offset = recyclerView.computeHorizontalScrollOffset()

                if (range - extent > 0) {
                    val progress = (100f * offset / (range - extent)).toInt()
                    progressScroll.progress = progress
                }
            }
        })

        // --- Configuración de Retrofit para conectar con la API ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.16:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Inicialización del adaptador de ofertas ---
        adapterOfertas = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewOfertas.adapter = adapterOfertas

        // --- Cargar ofertas desde la API ---
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()
                adapterOfertas.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- Configuración del RecyclerView de Categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Lista de categorías con sus imágenes y nombres
        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantánea"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Lácteos")
        )

        // Adaptador de categorías sin clics habilitados
        adapterCategorias = CategoriaAdaptador(
            categorias,
            onCategoriaClick = { /* No hace nada */ },
            clicHabilitado = false
        )
        recyclerViewCategorias.adapter = adapterCategorias

        // --- Configuración del RecyclerView de Tiendas Cercanas ---
        recyclerViewTiendas = view.findViewById(R.id.recyclerViewTiendas)
        recyclerViewTiendas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Inicializamos el adaptador de tiendas
        adapterTiendas = TiendaAdaptador(
            listaTiendas = emptyList(),
            context = requireContext(),
            layoutId = R.layout.actividad_vista_tienda,
            onTiendaClick = { tiendaSeleccionada ->
                Toast.makeText(
                    requireContext(),
                    "Seleccionaste ${tiendaSeleccionada.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        recyclerViewTiendas.adapter = adapterTiendas

        // --- Cargar tiendas desde la API ---
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tiendas = apiService.listarTiendas()
                adapterTiendas.actualizarLista(tiendas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- Configuración de botones ---
        val btnAdd: ImageButton = view.findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener {
            // Al presionar el botón, se abre el fragmento de notificaciones
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, NotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }

        val btnPerfil: ImageView = view.findViewById(R.id.btnPerfil)

        // --- Cargar imagen de perfil desde SharedPreferences ---
        val prefss =
            requireActivity().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val urlImagen = prefss.getString("urlImagen", null)

        if (!urlImagen.isNullOrEmpty()) {
            // Si hay una URL, cargamos la imagen con Glide
            Glide.with(this)
                .load(urlImagen)
                .placeholder(R.drawable.perfil)
                .error(R.drawable.perfil)
                .circleCrop()
                .into(btnPerfil)
        } else {
            // Si no hay imagen guardada, se muestra la imagen por defecto
            btnPerfil.setImageResource(R.drawable.perfil)
        }

        // --- Acción al presionar el botón de perfil ---
        btnPerfil.setOnClickListener {
            // Reemplaza el fragmento actual por el perfil del usuario
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Perfil_Usuario())
                .addToBackStack(null)
                .commit()
        }
        // Devuelve la vista raíz inflada
        return view
    }
}