package com.example.bestsellerfrontend

data class Producto(
    val id: String? = null,
    val nombre: String,
    val marca: Marca,
    val precio: Double,
    val urlImagen: String,
    val tiendaId: String,
    val usuarioId: String? = null
)
