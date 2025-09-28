package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bumptech.glide.Glide

class InicioUsuarioFragment : Fragment() {

    private lateinit var recyclerViewOfertas: RecyclerView
    private lateinit var adapterOfertas: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var ofertas: List<Oferta> = emptyList()

    private lateinit var recyclerViewCategorias: RecyclerView
    private lateinit var adapterCategorias: CategoriaAdaptador

    private lateinit var progressScroll: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_inicio_usuario, container, false)

        // --- Saludo ---
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
            //.baseUrl("http://10.0.2.2:8090/") // emulador
            .baseUrl("http://192.168.0.7:8090/") // tu backend real
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        adapterOfertas = OfertaAdaptador(emptyList(), requireContext(), apiService)
        recyclerViewOfertas.adapter = adapterOfertas

        // Llenar ofertas desde el backend
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
            Pair(R.drawable.precodidos, "Snacks"),
            Pair(R.drawable.granos, "Lácteos"),
            Pair(R.drawable.dulces, "Carnes")
        )

        adapterCategorias = CategoriaAdaptador(categorias)
        recyclerViewCategorias.adapter = adapterCategorias

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

        // Mostrar la foto del usuario si existe, si no, la imagen por defecto
        if (!urlImagen.isNullOrEmpty()) {
            Glide.with(this)
                .load(urlImagen)
                .placeholder(R.drawable.perfil) // mientras carga
                .error(R.drawable.perfil)       // si falla
                .circleCrop()                   // opcional: redonda
                .into(btnPerfil)
        } else {
            btnPerfil.setImageResource(R.drawable.perfil)
        }

        // Mantener tu listener para abrir el perfil
        btnPerfil.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Perfil_Usuario())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}