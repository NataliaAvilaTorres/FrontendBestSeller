package com.example.bestsellerfrontend

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part


interface ApiService {

    // -------------------------------
    // 🔹 MARCAS
    // -------------------------------

    /**
     * Obtiene la lista completa de marcas disponibles en el sistema.
     * @return Lista de objetos [Marca].
     */
    @GET("api/marcas/listar")
    suspend fun listarMarcas(): List<Marca>

    // -------------------------------
    // 🔹 USUARIOS
    // -------------------------------

    /**
     * Registra un nuevo usuario en la base de datos.
     * @param usuario Objeto con los datos del usuario a registrar.
     * @return Objeto [Respuesta] con el estado de la operación.
     */
    @POST("api/usuarios/registrar")
    suspend fun registrarUsuario(@Body usuario: Usuario): Respuesta

    /**
     * Inicia sesión de un usuario verificando sus credenciales.
     * @param usuario Contiene correo y contraseña.
     * @return Objeto [Respuesta] con el resultado del login.
     */
    @POST("api/usuarios/login")
    suspend fun login(@Body usuario: Usuario): Respuesta

    /**
     * Actualiza los datos de un usuario existente.
     * @param id ID del usuario.
     * @param usuario Datos actualizados del usuario.
     * @return Objeto [Respuesta] con el resultado.
     */
    @PUT("api/usuarios/actualizar/{id}")
    suspend fun actualizarUsuario(@Path("id") id: String, @Body usuario: Usuario): Respuesta

    /**
     * Elimina un usuario según su ID.
     * @param id ID del usuario.
     * @return Objeto [Respuesta].
     */
    @DELETE("api/usuarios/eliminar/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Respuesta

    // -------------------------------
    // 🔹 PRODUCTOS
    // -------------------------------

    /**
     * Obtiene todos los productos registrados.
     * @return Lista de objetos [Producto].
     */
    @GET("api/productos/listar")
    suspend fun listarProductos(): List<Producto>

    /**
     * Lista los productos asociados a un usuario específico.
     * @param usuarioId ID del usuario.
     * @return Lista de productos creados por ese usuario.
     */
    @GET("/api/productos/listar/{usuarioId}")
    suspend fun listarProductosUsuario(@Path("usuarioId") usuarioId: String): List<Producto>

    /**
     * Lista los productos pertenecientes a una tienda específica.
     * @param tiendaId ID de la tienda.
     * @return Lista de productos de esa tienda.
     */
    @GET("api/productos/listar/tienda/{tiendaId}")
    suspend fun listarProductosTienda(@Path("tiendaId") tiendaId: String): List<Producto>

    // -------------------------------
    // 🔹 OFERTAS
    // -------------------------------

    /**
     * Obtiene todas las ofertas registradas.
     * @return Lista de objetos [Oferta].
     */
    @GET("api/ofertas/listar")
    suspend fun listarOfertas(): List<Oferta>

    /**
     * Lista las ofertas creadas por un usuario específico.
     * @param usuarioId ID del usuario.
     * @return Lista de ofertas de ese usuario.
     */
    @GET("/api/ofertas/listar/{usuarioId}")
    suspend fun listarOfertasUsuario(@Path("usuarioId") usuarioId: String): List<Oferta>

    /**
     * Crea una nueva oferta asociada a un usuario.
     * @param usuarioId ID del usuario que la crea.
     * @param oferta Datos de la oferta.
     * @return Objeto [Respuesta] con el estado de la creación.
     */
    @POST("/api/ofertas/crear/{usuarioId}")
    suspend fun crearOferta(
        @Path("usuarioId") usuarioId: String,
        @Body oferta: Oferta
    ): Respuesta

    /**
     * Actualiza una oferta existente según su ID.
     * @param id ID de la oferta.
     * @param oferta Datos actualizados.
     * @return Objeto [Respuesta].
     */
    @PUT("api/ofertas/actualizar/{id}")
    suspend fun actualizarOferta(
        @Path("id") id: String,
        @Body oferta: Oferta
    ): Respuesta

    /**
     * Elimina una oferta del sistema según su ID.
     * @param id ID de la oferta.
     * @return Objeto [Respuesta].
     */
    @DELETE("api/ofertas/eliminar/{id}")
    suspend fun eliminarOferta(@Path("id") id: String): Respuesta

    /**
     * Permite dar o quitar "like" a una oferta específica.
     * @param ofertaId ID de la oferta.
     * @param usuarioId ID del usuario que interactúa.
     * @param liked Valor booleano que indica si el usuario dio o quitó el like.
     * @return Objeto [Respuesta].
     */
    @POST("api/ofertas/{id}/like/{usuarioId}")
    suspend fun toggleLike(
        @Path("id") ofertaId: String,
        @Path("usuarioId") usuarioId: String,
        @Query("liked") liked: Boolean
    ): Respuesta

    // -------------------------------
    // 🔹 TIENDAS
    // -------------------------------

    /**
     * Crea una nueva tienda.
     * @param tienda Datos de la tienda.
     * @return Objeto [Respuesta].
     */
    @POST("api/tiendas/crear")
    suspend fun crearTienda(@Body tienda: Tienda): Respuesta

    /**
     * Lista todas las tiendas registradas.
     * @return Lista de objetos [Tienda].
     */
    @GET("api/tiendas/listar")
    suspend fun listarTiendas(): List<Tienda>

    /**
     * Obtiene una tienda específica por su ID.
     * @param id ID de la tienda.
     * @return Objeto [Tienda].
     */
    @GET("api/tiendas/{id}")
    suspend fun obtenerTienda(@Path("id") id: String): Tienda

    /**
     * Actualiza los datos de una tienda existente.
     * @param id ID de la tienda.
     * @param tienda Datos actualizados.
     * @return Objeto [Respuesta].
     */
    @PUT("api/tiendas/actualizar/{id}")
    suspend fun actualizarTienda(@Path("id") id: String, @Body tienda: Tienda): Respuesta

    /**
     * Elimina una tienda del sistema.
     * @param id ID de la tienda.
     * @return Objeto [Respuesta].
     */
    @DELETE("api/tiendas/eliminar/{id}")
    suspend fun eliminarTienda(@Path("id") id: String): Respuesta

    // -------------------------------
    // 🔹 NOTIFICACIONES
    // -------------------------------

    /**
     * Lista todas las notificaciones registradas.
     * @return Lista de objetos [Notificacion].
     */
    @GET("/api/notificaciones/listar")
    suspend fun listarNotificaciones(): List<Notificacion>

    // -------------------------------
    // 🔹 GOOGLE PLACES (Lugares cercanos)
    // -------------------------------

    /**
     * Busca lugares cercanos como supermercados usando la API de Google Places.
     * @param ubicacion Coordenadas en formato "lat,lng".
     * @param radio Radio de búsqueda en metros.
     * @param tipo Tipo de lugar (por defecto "supermarket").
     * @param apiKey Clave de API de Google.
     * @return Objeto [RespuestaLugares] con la información de los lugares encontrados.
     */
    @GET("maps/api/place/nearbysearch/json")
    suspend fun buscarLugaresCercanos(
        @Query("location") ubicacion: String,
        @Query("radius") radio: Int,
        @Query("type") tipo: String = "supermarket",
        @Query("key") apiKey: String
    ): RespuestaLugares

    // -------------------------------
    // 🔹 NUTRICIÓN (Google Cloud Vision)
    // -------------------------------

    /**
     * Envía una imagen de tabla nutricional al backend para ser analizada
     * usando Google Cloud Vision API.
     * @param image Imagen en formato Multipart
     * @return Objeto [AnalisisNutricional] con nutrientes detectados y recomendaciones
     */
    @Multipart
    @POST("api/nutricion/analizar")
    suspend fun analizarTablaNutricional(
        @Part image: MultipartBody.Part
    ): AnalisisNutricional

    @GET("api/usuarios/listar")
    suspend fun listarUsuarios(): List<Usuario>
}