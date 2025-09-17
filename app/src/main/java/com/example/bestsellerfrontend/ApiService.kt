package com.example.bestsellerfrontend

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query


public interface ApiService {

    @POST("api/usuarios/registrar")
    suspend fun registrarUsuario(@Body usuario: Usuario): Respuesta

    @POST("api/usuarios/login")
    suspend fun login(@Body usuario: Usuario): Respuesta

    @GET("api/productos/listar")
    suspend fun listarProductos(): List<Producto>

    @GET("api/ofertas/listar")
    suspend fun listarOfertas(): List<Oferta>

    @POST("api/ofertas/crear")
    suspend fun crearOferta(@Body oferta: Oferta): Respuesta

    @POST("api/ofertas/toggleLike")
    suspend fun toggleLike(
        @Query("id") id: String,
        @Query("nuevoEstado") nuevoEstado: Boolean
    ): Respuesta

    @PUT("api/usuarios/actualizar/{id}")
    suspend fun actualizarUsuario(@Path("id") id: String, @Body usuario: Usuario): Respuesta

    @DELETE("api/usuarios/eliminar/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Respuesta

}