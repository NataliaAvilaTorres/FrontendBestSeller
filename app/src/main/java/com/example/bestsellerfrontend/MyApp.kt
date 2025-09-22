package com.example.bestsellerfrontend

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 🚀 Inicializar Firebase manualmente (sin google-services.json)
        val options = FirebaseOptions.Builder()
            .setApplicationId("1:948510083346:android:bd75f0baa4dda045a6cbcb") // 👈 Tu App ID de Firebase
            .setApiKey("AlzaSyA4dTDmgE8AquVklzKWMbJ6XMxiLyZqXhw") // 👈 Tu API key
            .setDatabaseUrl("https://best-seller-382e9-default-rtdb.firebaseio.com/") // 👈 Tu Realtime Database
            .build()

        // Evita inicializar dos veces
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }
    }
}
