package com.example.bestsellerfrontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Actividad_Navegacion_Admin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barra_navegacion_admin)

        // Obtiene la referencia a la vista de navegación inferior (BottomNavigationView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)


        // Listener que maneja los clics en los ítems de la barra de navegación
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dashboard -> {  // ← Agregar esta opción
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedorAdmin, DashboardAdminFragment())
                        .commit()
                    true
                }
                R.id.usuarios -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.contenedorAdmin,
                            ListaUsuariosFragment()
                        ) // Reemplaza el fragmento actual
                        .addToBackStack(null) // Permite volver al fragmento anterior
                        .commit()
                    true
                }
                R.id.inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedorAdmin, Fragment_Inicio_Admin())
                        .commit()
                    true
                }
                R.id.publicaciones -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedorAdmin, ListaPublicacionesFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.ajustes -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedorAdmin, FragmentActividadAjustesAdmin())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                // Si ninguna opción coincide, no se realiza ninguna acción
                else -> false
            }
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedorAdmin, Fragment_Inicio_Admin())
                .commit()
            bottomNav.selectedItemId = R.id.inicio
        }
    }
}