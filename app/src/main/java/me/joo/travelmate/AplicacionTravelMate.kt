package me.joo.travelmate

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import me.joo.travelmate.datos.local.BaseDatosTravelMate
import me.joo.travelmate.datos.repositorio.RepositorioLugares
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration

class AplicacionTravelMate : Application(), ImageLoaderFactory {
    val baseDatos by lazy {
        Room.databaseBuilder(this, BaseDatosTravelMate::class.java, "travelmate.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val repositorio by lazy { RepositorioLugares(baseDatos.daoFavoritos()) }

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "TravelMateApp/1.0 (Android; imagenes de destinos)")
                            .build()
                    )
                }
                .build()
        }
        .crossfade(true)
        .build()
}
