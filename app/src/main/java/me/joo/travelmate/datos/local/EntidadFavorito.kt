package me.joo.travelmate.datos.local
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoritos")
data class EntidadFavorito(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val imagen: String?,
    val wikipedia: String?,
    val categoria: String = "Sin categoría",
    val departamento: String = "Sin departamento",
    val horario: String? = null,
    val telefono: String? = null,
    val sitioWeb: String? = null,
    val calificacion: String? = null
)
