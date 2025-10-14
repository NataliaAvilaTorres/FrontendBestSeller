package com.example.bestsellerfrontend

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Actividad_Navegacion_Usuario : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barra_navegacion)

        // Obtiene la referencia a la vista de navegación inferior (BottomNavigationView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Listener que maneja los clics en los ítems de la barra de navegación
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                // Opción "Buscar" → abre el fragmento con la lista de productos
                R.id.buscar -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.contenedor,
                            ListaProductosFragment()
                        ) // Reemplaza el fragmento actual
                        .addToBackStack(null) // Permite volver al fragmento anterior
                        .commit()
                    true
                }

                // Opción "Inicio" → muestra el fragmento principal del usuario
                R.id.inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, InicioUsuarioFragment())
                        .commit()
                    true
                }

                // Opción "Oferta" → muestra el fragmento con las ofertas disponibles
                R.id.oferta -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, ListaOfertasFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                // Opción "Ubicación" → abre el fragmento con el mapa
                R.id.ubicacion -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, MapaFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                // Opción "Cámara" → abre una nueva actividad para el reconocimiento de productos
                R.id.camara -> {
                    val intent = Intent(this, ReconocimientoActivity::class.java)
                    startActivity(intent)
                    true
                }

                // Si ninguna opción coincide, no se realiza ninguna acción
                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor, InicioUsuarioFragment())
                .commit()
            bottomNav.selectedItemId = R.id.inicio
        }
    }
}