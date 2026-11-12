package me.joo.travelmate.datos.remoto
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiGeoapify {
    @GET("v2/places")
    suspend fun consultar(
        @Query("categories") categorias: String,
        @Query("filter") filtro: String,
        @Query("bias") preferencia: String? = null,
        @Query("limit") limite: Int = 30,
        @Query("lang") idioma: String = "es",
        @Query("apiKey") clave: String
    ): JsonObject
}
