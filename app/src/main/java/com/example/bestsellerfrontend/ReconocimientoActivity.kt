package com.example.bestsellerfrontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ReconocimientoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reconocimiento)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor, ReconocimientoFragment())
                .commit()
        }
    }
}
