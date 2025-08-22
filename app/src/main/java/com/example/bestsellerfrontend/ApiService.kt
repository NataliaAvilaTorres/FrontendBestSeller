package com.example.bestsellerfrontend

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

public interface ApiService {

    @POST("api/usuarios/registrar")
    suspend fun registrarUsuario(@Body usuario: Usuario)

    @POST("api/usuarios/login")
    suspend fun login(@Body usuario: Usuario): Respuesta
}