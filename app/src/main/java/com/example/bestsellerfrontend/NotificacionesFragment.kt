package com.example.bestsellerfrontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class NotificacionesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificacionAdaptador
    private lateinit var notificacionesRef: DatabaseReference
    private val lista = mutableListOf<Notificacion>()

    data class NotificacionFirebase(
        val id: String? = null,
        val usuario: String? = null, // 👈 Aquí viene el usuarioId
        val mensaje: String? = null,
        val timestamp: Long? = null,
        val idOferta: String? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificacionAdaptador(lista)
        recyclerView.adapter = adapter

        val btnAtras = view.findViewById<ImageView>(R.id.btn_atras)
        btnAtras.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 🚀 Referencia a notificaciones
        notificacionesRef = FirebaseDatabase.getInstance().getReference("notificaciones")

        // Cargar notificaciones en tiempo real
        cargarNotificaciones()

        return view
    }

    private fun cargarNotificaciones() {
        notificacionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lista.clear()

                for (child in snapshot.children) {
                    val notif = child.getValue(NotificacionFirebase::class.java)

                    if (notif != null) {
                        val usuarioId = notif.usuario ?: ""

                        // 🔍 Buscar el nombre real en la rama usuarios
                        val usuarioRef = FirebaseDatabase.getInstance()
                            .getReference("usuarios")
                            .child(usuarioId)

                        usuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val nombreUsuario = userSnapshot.child("nombre")
                                    .getValue(String::class.java) ?: "Usuario"

                                // Crear notificación con nombre
                                val notificacionConNombre = Notificacion(
                                    id = notif.id,
                                    usuario = nombreUsuario,
                                    mensaje = notif.mensaje,
                                    timestamp = notif.timestamp,
                                    idOferta = notif.idOferta
                                )

                                lista.add(notificacionConNombre)

                                // 🔄 Ordenar por fecha (más recientes primero)
                                lista.sortByDescending { it.timestamp }

                                // Notificar cambios al adaptador
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}