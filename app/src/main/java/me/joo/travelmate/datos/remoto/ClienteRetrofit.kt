package me.joo.travelmate.datos.remoto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ClienteRetrofit {
    val api: ApiGeoapify = Retrofit.Builder()
        .baseUrl("https://api.geoapify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiGeoapify::class.java)
}
