package com.example.bestsellerfrontend

data class AnalisisNutricional(
    val nutrientes: Map<String, Int>,
    val evaluacion: String,
    val recomendaciones: List<String>,
    val texto_extraido: String? = null
)
