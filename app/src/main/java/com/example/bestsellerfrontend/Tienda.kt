package com.example.bestsellerfrontend

import java.io.Serializable

data class Tienda(
    val id: String = "",
    val nombre: String,
    val urlImagen: String,
    val ubicacion: Ubicacion? = null
) : Serializable
