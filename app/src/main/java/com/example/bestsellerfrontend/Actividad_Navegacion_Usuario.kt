package com.example.bestsellerfrontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Actividad_Navegacion_Usuario : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barra_navegacion)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.buscar -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, ListaProductosFragment())
                        .commit()
                    true
                }
                R.id.inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, InicioUsuarioFragment())
                        .commit()
                    true
                }
                R.id.oferta -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.contenedor, ListaOfertasFragment())
                        .commit()
                    true
                }
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
