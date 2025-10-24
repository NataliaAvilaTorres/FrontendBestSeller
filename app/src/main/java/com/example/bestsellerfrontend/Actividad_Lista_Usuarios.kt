package com.example.bestsellerfrontend

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ListaUsuariosFragment : Fragment() {

    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var adaptador: UsuarioAdaptador
    private lateinit var apiService: ApiService
    private val listaUsuarios = mutableListOf<Usuario>()

    private val TAG = "ListaUsuariosFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView iniciado ✅")

        // Inflar la vista del fragmento
        val view = inflater.inflate(R.layout.actividad_lista_usuarios, container, false)

        // --- Botón regresar ---
        val btnRegresar = view.findViewById<ImageView>(R.id.btnRegresar)
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // --- Configurar RecyclerView ---
        recyclerUsuarios = view.findViewById(R.id.recyclerUsuarios)
        recyclerUsuarios.layoutManager = LinearLayoutManager(requireContext()) // ✅ Muy importante
        adaptador = UsuarioAdaptador(mutableListOf()) { usuario ->
            eliminarUsuario(usuario)
        }
        recyclerUsuarios.adapter = adaptador


        // --- Inicializar Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // localhost emulador
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Cargar lista de usuarios desde API ---
        cargarUsuarios()

        return view
    }

    private fun cargarUsuarios() {
        lifecycleScope.launch {
            try {
                val respuesta = apiService.listarUsuarios()
                Log.d(TAG, "Respuesta API: $respuesta")

                if (respuesta.isNotEmpty()) {
                    adaptador.actualizarLista(respuesta) // ✅ directamente
                } else {
                    Toast.makeText(requireContext(), "No hay usuarios registrados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuarios", e)
                Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarUsuario(usuario: Usuario) {
        lifecycleScope.launch {
            try {
                val idUsuario = usuario.id
                if (idUsuario != null) {
                    val respuesta = apiService.eliminarUsuario(idUsuario)
                    Toast.makeText(requireContext(), respuesta.mensaje, Toast.LENGTH_SHORT).show()
                    listaUsuarios.remove(usuario)
                    adaptador.actualizarLista(listaUsuarios)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al eliminar usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}