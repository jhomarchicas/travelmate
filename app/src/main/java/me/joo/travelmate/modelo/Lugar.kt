package me.joo.travelmate.modelo

data class Lugar(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val distanciaMetros: Int? = null,
    val imagen: String? = null,
    val wikipedia: String? = null,
    val categoria: String = "Sin categoría",
    val departamento: String = "Sin departamento",
    val horario: String? = null,
    val telefono: String? = null,
    val sitioWeb: String? = null,
    val calificacion: String? = null
)
