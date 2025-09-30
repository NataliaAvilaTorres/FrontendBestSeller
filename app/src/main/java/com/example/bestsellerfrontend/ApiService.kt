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

    @GET("/api/productos/listar/{usuarioId}")
    suspend fun listarProductosUsuario(@Path("usuarioId") usuarioId: String): List<Producto>


    @GET("api/ofertas/listar")
    suspend fun listarOfertas(): List<Oferta>

    @GET("/api/ofertas/listar/{usuarioId}")
    suspend fun listarOfertasUsuario(@Path("usuarioId") usuarioId: String): List<Oferta>

    @POST("/api/ofertas/crear/{usuarioId}")
    suspend fun crearOferta(
        @Path("usuarioId") usuarioId: String,
        @Body oferta: Oferta
    ): Respuesta


    @POST("api/ofertas/{id}/like/{usuarioId}")
    suspend fun toggleLike(
        @Path("id") ofertaId: String,
        @Path("usuarioId") usuarioId: String,
        @Query("liked") liked: Boolean
    ): Respuesta


    @PUT("api/ofertas/actualizar/{id}")
    suspend fun actualizarOferta(
        @Path("id") id: String,
        @Body oferta: Oferta
    ): Respuesta

    @DELETE("api/ofertas/eliminar/{id}")
    suspend fun eliminarOferta(
        @Path("id") id: String
    ): Respuesta


    @PUT("api/usuarios/actualizar/{id}")
    suspend fun actualizarUsuario(@Path("id") id: String, @Body usuario: Usuario): Respuesta

    @DELETE("api/usuarios/eliminar/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Respuesta

    // 🔹 ENDPOINT DE GOOGLE PLACES (TIENDAS, SUPERMERCADOS CERCANOS)
    @GET("maps/api/place/nearbysearch/json")
    suspend fun buscarLugaresCercanos(
        @Query("location") ubicacion: String,
        @Query("radius") radio: Int,
        @Query("type") tipo: String = "supermarket",
        @Query("key") apiKey: String
    ): RespuestaLugares

    @GET("/api/notificaciones/listar")
    suspend fun listarNotificaciones(): List<Notificacion>

//TIENDASS
    @POST("api/tiendas/crear")
    suspend fun crearTienda(@Body tienda: Tienda): Respuesta

    @GET("api/tiendas/listar")
    suspend fun listarTiendas(): List<Tienda>

    @GET("api/tiendas/{id}")
    suspend fun obtenerTienda(@Path("id") id: String): Tienda

    @PUT("api/tiendas/actualizar/{id}")
    suspend fun actualizarTienda(@Path("id") id: String, @Body tienda: Tienda): Respuesta

    @DELETE("api/tiendas/eliminar/{id}")
    suspend fun eliminarTienda(@Path("id") id: String): Respuesta
}