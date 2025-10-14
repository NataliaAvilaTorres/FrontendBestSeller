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
        val usuario: String? = null,
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

        // Inicializar el RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Configurar el adaptador con una lista vacía
        adapter = NotificacionAdaptador(lista)
        recyclerView.adapter = adapter

        // Configurar el botón "Atrás" para volver a la pantalla anterior
        val btnAtras = view.findViewById<ImageView>(R.id.btn_atras)
        btnAtras.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Referencia al nodo "notificaciones" en Firebase
        notificacionesRef = FirebaseDatabase.getInstance().getReference("notificaciones")

        // Cargar las notificaciones desde Firebase
        cargarNotificaciones()

        // Retornar la vista completa del fragmento
        return view
    }

    // Función que obtiene las notificaciones en tiempo real desde Firebase
    private fun cargarNotificaciones() {
        // Escucha los cambios en el nodo de notificaciones
        notificacionesRef.addValueEventListener(object : ValueEventListener {

            // Se ejecuta cada vez que hay un cambio en los datos
            override fun onDataChange(snapshot: DataSnapshot) {
                // Limpiar la lista antes de volver a llenarla
                lista.clear()

                // Recorrer cada notificación dentro del snapshot
                for (child in snapshot.children) {
                    // Obtener el objeto de notificación desde Firebase
                    val notif = child.getValue(NotificacionFirebase::class.java)

                    // Si la notificación existe, continuar
                    if (notif != null) {
                        val usuarioId = notif.usuario ?: ""

                        // Obtener la referencia al usuario que generó la notificación
                        val usuarioRef = FirebaseDatabase.getInstance()
                            .getReference("usuarios")
                            .child(usuarioId)

                        // Obtener los datos del usuario para mostrar su nombre
                        usuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                // Obtener el nombre del usuario o usar "Usuario" por defecto
                                val nombreUsuario = userSnapshot.child("nombre")
                                    .getValue(String::class.java) ?: "Usuario"

                                // Crear un objeto Notificacion con el nombre del usuario
                                val notificacionConNombre = Notificacion(
                                    id = notif.id,
                                    usuario = nombreUsuario,
                                    mensaje = notif.mensaje,
                                    timestamp = notif.timestamp,
                                    idOferta = notif.idOferta
                                )

                                // Agregar la notificación a la lista
                                lista.add(notificacionConNombre)

                                // Ordenar las notificaciones por fecha descendente
                                lista.sortByDescending { it.timestamp }

                                // Notificar al adaptador que los datos cambiaron
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // No se hace nada si la lectura del usuario fue cancelada
                            }
                        })
                    }
                }
            }
            // Se ejecuta si hay un error al leer los datos de Firebase
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}