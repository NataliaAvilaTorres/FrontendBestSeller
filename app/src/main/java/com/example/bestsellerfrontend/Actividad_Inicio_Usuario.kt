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

        val txtSaludo: TextView = view.findViewById(R.id.txtSaludo)
        val prefs =
            requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre", "Usuario")
        txtSaludo.text = "Hola $nombreUsuario, Buen día!"

        // --- RecyclerView de Ofertas ---
        recyclerViewOfertas = view.findViewById(R.id.recyclerViewOfertas)
        recyclerViewOfertas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        progressScroll = view.findViewById(R.id.progressScroll)
        recyclerViewOfertas.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val extent = recyclerView.computeHorizontalScrollExtent()
                val range = recyclerView.computeHorizontalScrollRange()
                val offset = recyclerView.computeHorizontalScrollOffset()

                if (range - extent > 0) {
                    val progress = (100f * offset / (range - extent)).toInt()
                    progressScroll.progress = progress
                }
            }
        })

        // --- API Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // emulador
            //.baseUrl("http://192.168.0.7:8090/") // tu backend real
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Ofertas
        adapterOfertas = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewOfertas.adapter = adapterOfertas

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ofertas = apiService.listarOfertas()
                adapterOfertas.actualizarLista(ofertas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- RecyclerView de Categorías ---
        recyclerViewCategorias = view.findViewById(R.id.recyclerViewCategorias)
        recyclerViewCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categorias: List<Pair<Int, String>> = listOf(
            Pair(R.drawable.bebida, "Bebidas"),
            Pair(R.drawable.enlatados, "Enlatados"),
            Pair(R.drawable.granos, "Granos"),
            Pair(R.drawable.precodidos, "Instantáneoa"),
            Pair(R.drawable.dulces, "Dulces"),
            Pair(R.drawable.pastasyharinas, "Lácteos")
        )

        adapterCategorias = CategoriaAdaptador(
            categorias,
            onCategoriaClick = { /* No hace nada */ },
            clicHabilitado = false // 🔹 deshabilita clics
        )

        recyclerViewCategorias.adapter = adapterCategorias

// --- RecyclerView de Tiendas Cercanas ---
        recyclerViewTiendas = view.findViewById(R.id.recyclerViewTiendas)
        recyclerViewTiendas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

// Usamos el layout original para esta actividad
        adapterTiendas = TiendaAdaptador(
            listaTiendas = emptyList(),
            context = requireContext(),
            layoutId = R.layout.actividad_vista_tienda, // ✅ importante
            onTiendaClick = { tiendaSeleccionada ->
                Toast.makeText(requireContext(), "Seleccionaste ${tiendaSeleccionada.nombre}", Toast.LENGTH_SHORT).show()
                // Aquí puedes agregar acción adicional si quieres
            }
        )

        recyclerViewTiendas.adapter = adapterTiendas

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tiendas = apiService.listarTiendas()
                adapterTiendas.actualizarLista(tiendas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // --- Botones ---
        val btnAdd: ImageButton = view.findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, NotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }

        val btnPerfil: ImageView = view.findViewById(R.id.btnPerfil)

        // Recuperar la URL guardada en SharedPreferences
        val prefss = requireActivity().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val urlImagen = prefss.getString("urlImagen", null)

        if (!urlImagen.isNullOrEmpty()) {
            Glide.with(this)
                .load(urlImagen)
                .placeholder(R.drawable.perfil)
                .error(R.drawable.perfil)
                .circleCrop()
                .into(btnPerfil)
        } else {
            btnPerfil.setImageResource(R.drawable.perfil)
        }

        btnPerfil.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Perfil_Usuario())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
