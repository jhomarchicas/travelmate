package me.joo.travelmate.datos.remoto

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiWikipedia {
    @GET("api/rest_v1/page/media-list/{titulo}")
    suspend fun media(
        @Path("titulo", encoded = true) titulo: String,
        @Header("Accept") accept: String = "application/json"
    ): JsonObject

    @GET("w/api.php")
    suspend fun buscarPagina(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("srsearch") consulta: String,
        @Query("srlimit") limite: Int = 1,
        @Query("format") format: String = "json",
        @Query("utf8") utf8: Int = 1
    ): JsonObject

    @GET("w/api.php")
    suspend fun buscarImagenesCommons(
        @Query("action") action: String = "query",
        @Query("generator") generator: String = "search",
        @Query("gsrsearch") consulta: String,
        @Query("gsrnamespace") namespace: Int = 6,
        @Query("gsrlimit") limite: Int = 8,
        @Query("prop") prop: String = "imageinfo",
        @Query("iiprop") iiprop: String = "url",
        @Query("iiurlwidth") ancho: Int = 800,
        @Query("format") format: String = "json",
        @Query("utf8") utf8: Int = 1
    ): JsonObject
}
