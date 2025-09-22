package com.example.bestsellerfrontend

import android.app.AlertDialog
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
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
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

    private lateinit var progressScroll: ProgressBar

    // Firebase
    private lateinit var notificacionesRef: DatabaseReference
    private var childListener: ChildEventListener? = null

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

        // 🚀 Inicializar Firebase si aún no está
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            FirebaseApp.initializeApp(requireContext())
        }

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

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/") // emulador
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

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
        btnPerfil.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor, Actividad_Perfil_Usuario())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        notificacionesRef = FirebaseDatabase.getInstance().getReference("notificaciones")

        childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val usuario = snapshot.child("usuario").getValue(String::class.java)
                val mensaje = snapshot.child("mensaje").getValue(String::class.java)

                // ✅ Usamos context? para no crashear si el fragment ya no está
                context?.let { ctx ->
                    if (usuario != null && mensaje != null) {
                        AlertDialog.Builder(ctx)
                            .setTitle("📢 Nueva oferta")
                            .setMessage("$usuario publicó: $mensaje")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        notificacionesRef.limitToLast(1).addChildEventListener(childListener!!)
    }

    override fun onStop() {
        super.onStop()
        // ✅ Quitamos el listener al salir
        childListener?.let { notificacionesRef.removeEventListener(it) }
        childListener = null
    }
}
