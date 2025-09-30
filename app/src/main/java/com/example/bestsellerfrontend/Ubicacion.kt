package com.example.bestsellerfrontend

import java.io.Serializable

data class Ubicacion(
    val lat: Double,
    val lng: Double,
    val direccion: String? = null
) : Serializable
