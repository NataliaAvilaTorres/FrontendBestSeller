package com.example.bestsellerfrontend

import java.util.Date

data class Oferta(
    val nombreOferta: String,
    val descripcionOferta: String,
    val tiendaNombre: String,
    val fechaOferta: Date,
    val producto: Producto,
    val urlImagen: String)

