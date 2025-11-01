package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.TextView
import android.widget.Toast


class Actividad_Ver_Publicaciones : Fragment() {

    // --- Variables principales ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfertaAdaptador
    private lateinit var apiService: ApiService
    private var publicaciones: List<Oferta> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.actividad_ver_publicaciones, container, false)

        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- CONFIGURAR RECYCLER VIEW ---
        recyclerView = view.findViewById(R.id.recyclerMisPublicaciones)
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext()) // Organiza los elementos en una lista vertical

        // --- CONFIGURAR RETROFIT PARA CONECTAR CON EL BACKEND ---
        val retrofit = Retrofit.Builder()
            //.baseUrl("http://10.0.2.2:8090/")
            .baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Inicializa el servicio API
        apiService = retrofit.create(ApiService::class.java)

        // Crea el adaptador vacío inicialmente
        adapter = OfertaAdaptador(emptyList(), requireContext(), apiService, mostrarBotones = true)
        recyclerView.adapter = adapter

        // --- OBTENER ID DEL USUARIO DESDE SharedPreferences ---
        val prefs =
            requireContext().getSharedPreferences("usuarioPrefs", AppCompatActivity.MODE_PRIVATE)
        val usuarioId =
            prefs.getString("id", null) // Recupera el ID guardado durante el login o registro

        // --- CARGAR PUBLICACIONES DEL USUARIO ---
        if (usuarioId != null) {
            // Llamada asincrónica con corrutinas para no bloquear la interfaz
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Obtiene las publicaciones del usuario desde el backend
                    publicaciones = apiService.listarOfertasUsuario(usuarioId)
                    // Actualiza el adaptador con los datos recibidos
                    adapter.actualizarLista(publicaciones)
                } catch (e: Exception) {
                    e.printStackTrace() // Imprime el error en el log
                }
            }
        }

        // --- TEXTVIEWS PARA MOSTRAR ESTADÍSTICAS ---
        val tvNumeroPublicaciones = view.findViewById<TextView>(R.id.tvNumeroPublicaciones)
        val tvLikesRecibidos = view.findViewById<TextView>(R.id.tvLikesRecibidos)

        // --- CARGAR NÚMERO DE PUBLICACIONES Y TOTAL DE LIKES ---
        if (usuarioId != null) {
            lifecycleScope.launch {
                try {
                    // Obtiene nuevamente las publicaciones (para asegurar datos actualizados)
                    publicaciones = apiService.listarOfertasUsuario(usuarioId)
                    adapter.actualizarLista(publicaciones)

                    // Muestra la cantidad total de publicaciones
                    tvNumeroPublicaciones.text = publicaciones.size.toString()

                    // Calcula la suma de likes recibidos en todas las publicaciones
                    val totalLikes = publicaciones.sumOf { it.likes }
                    tvLikesRecibidos.text = totalLikes.toString()

                } catch (e: Exception) {
                    // Si ocurre un error en la conexión o procesamiento
                    e.printStackTrace()
                }
            }
        } else {
            // Si no se encuentra un usuario logueado, muestra un mensaje de error
            Toast.makeText(requireContext(), "Error: usuario no logueado", Toast.LENGTH_SHORT)
                .show()
        }

        // Retorna la vista creada para que se muestre en pantalla
        return view
    }
}