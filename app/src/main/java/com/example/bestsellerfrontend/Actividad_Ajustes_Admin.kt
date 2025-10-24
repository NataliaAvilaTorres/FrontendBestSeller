package com.example.bestsellerfrontend

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class FragmentActividadAjustesAdmin : Fragment() {

    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvContrasena: TextView
    private lateinit var btnCerrarSesion: MaterialButton
    private lateinit var btnRegresar: ImageView

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.actividad_ajustes_admin, container, false)

        // Referencias de vista
        tvNombre = view.findViewById(R.id.tvNombre)
        tvCorreo = view.findViewById(R.id.tvCorreo)
        tvContrasena = view.findViewById(R.id.tvContrasena)
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion)
        btnRegresar = view.findViewById(R.id.btnRegresar)

        // Inicializa SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Cargar datos del usuario guardados
        mostrarDatosUsuario()

        // Botón regresar
        btnRegresar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Botón cerrar sesión
        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        return view
    }

    private fun mostrarDatosUsuario() {
        val nombre = sharedPreferences.getString("nombre", "Administrador")
        val correo = sharedPreferences.getString("correo", "admin@gmail.com")
        val contrasena = sharedPreferences.getString("contrasena", "********")

        tvNombre.text = nombre
        tvCorreo.text = correo
        tvContrasena.text = contrasena
    }

    private fun cerrarSesion() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Si luego tienes un login activity o fragment, aquí podrías redirigirlo
        // Ejemplo:
        // val intent = Intent(requireContext(), LoginActivity::class.java)
        // startActivity(intent)
        // requireActivity().finish()
    }
}