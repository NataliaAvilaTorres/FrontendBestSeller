package com.example.bestsellerfrontend

data class Producto(
    val id: String? = null,
    val nombre: String,
    val categoria: String,
    val marca: String,
    val precio: Double,
    val urlImagen: String,
    val usuarioId: String? = null
)
