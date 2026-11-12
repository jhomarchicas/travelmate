package me.joo.travelmate.datos.local
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntidadFavorito::class], version = 3, exportSchema = false)
abstract class BaseDatosTravelMate : RoomDatabase() {
    abstract fun daoFavoritos(): DaoFavoritos
}
