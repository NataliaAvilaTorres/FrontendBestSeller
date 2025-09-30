package com.example.bestsellerfrontend

import java.io.Serializable

data class Oferta(
    val id: String = "",
    val nombreOferta: String,
    val descripcionOferta: String,
    val tiendaId: String,
    val fechaOferta: Long,
    val producto: Producto,
    val urlImagen: String,
    var likes: Int = 0,
    var likedByUser: Boolean = false,
    val usuarioId: String? = null,
    val productoId: String? = null,
    var likedBy: Map<String, Boolean> = emptyMap(),
    val ubicacion: Ubicacion? = null,

    var usuarioNombre: String? = null,
    var usuarioFoto: String? = null
) : Serializable
