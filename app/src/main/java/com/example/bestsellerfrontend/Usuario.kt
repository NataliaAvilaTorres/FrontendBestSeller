package com.example.bestsellerfrontend

data class Usuario(
    val nombre: String,
    val correo: String,
    val ciudad: String,
    val contrasena: String,
    val id: String? = null
)

