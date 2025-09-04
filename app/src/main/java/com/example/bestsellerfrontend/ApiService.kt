package com.example.bestsellerfrontend

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

public interface ApiService {

    @POST("api/usuarios/registrar")
    suspend fun registrarUsuario(@Body usuario: Usuario)

    @POST("api/usuarios/login")
    suspend fun login(@Body usuario: Usuario): Respuesta

    @GET("api/productos/listar")
    suspend fun listarProductos(): List<Producto>

    @GET("api/ofertas/listar")
    suspend fun listarOfertas(): List<Oferta>


    @POST("api/ofertas/crear")
    suspend fun crearOferta(@Body oferta: Oferta): Respuesta

}