package me.joo.travelmate.datos.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoFavoritos {
    @Query("SELECT * FROM favoritos ORDER BY nombre") fun observarTodos(): Flow<List<EntidadFavorito>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardar(favorito: EntidadFavorito)
    @Query("DELETE FROM favoritos WHERE id = :id") suspend fun eliminar(id: String)
}
