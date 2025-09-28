package com.example.bestsellerfrontend

import android.os.Bundle
import android.content.Intent
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
                        .addToBackStack(null)
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
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.ubicacion -> {
                    val intent = Intent(this, Actividad_Mapa::class.java)
                    startActivity(intent)
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
