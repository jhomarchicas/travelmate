package me.joo.travelmate.datos.remoto

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ClienteWikipedia {
    private val cliente = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "TravelMateApp/1.0 (Android; contacto: app local)")
                    .build()
            )
        }
        .build()

    fun api(idioma: String = "es"): ApiWikipedia = Retrofit.Builder()
        .baseUrl("https://$idioma.wikipedia.org/")
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiWikipedia::class.java)

    val commons: ApiWikipedia = Retrofit.Builder()
        .baseUrl("https://commons.wikimedia.org/")
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiWikipedia::class.java)
}
