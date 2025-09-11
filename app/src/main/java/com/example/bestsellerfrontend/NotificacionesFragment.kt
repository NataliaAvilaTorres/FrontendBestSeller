package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificacionesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificacionAdaptador

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones, container, false)

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Datos de prueba
        val listaNotificaciones = listOf(
            Notificacion("Laura Pérez", "🎉 Oferta en granos - Pack x4", "Hace 1 h"),
            Notificacion("Juan Gómez", "🔥 Descuento en bebidas", "Hace 3 h"),
            Notificacion("María López", "🥫 Nueva oferta en enlatados", "Hace 1 día"),
            Notificacion("Pedro Ruiz", "🍫 Promoción en dulces", "Hace 2 días")
        )

        // Conectar adaptador
        adapter = NotificacionAdaptador(listaNotificaciones)
        recyclerView.adapter = adapter

        // Botón de volver
        val btnAtras = view.findViewById<ImageView>(R.id.btn_atras)
        btnAtras.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }


        return view
    }
}
