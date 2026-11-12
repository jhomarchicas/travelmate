package me.joo.travelmate.datos.repositorio

import android.net.Uri
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.joo.travelmate.BuildConfig
import me.joo.travelmate.datos.local.*
import me.joo.travelmate.datos.remoto.ClienteRetrofit
import me.joo.travelmate.datos.remoto.ClienteWikipedia
import me.joo.travelmate.modelo.DatosElSalvador
import me.joo.travelmate.modelo.Lugar

class RepositorioLugares(private val dao: DaoFavoritos) {
    val favoritos: Flow<List<Lugar>> = dao.observarTodos().map { lista -> lista.map { it.aLugar() } }

    suspend fun buscarCercanos(
        latitud: Double,
        longitud: Double,
        radio: Int = 12000,
        categoriasApi: String = DatosElSalvador.categorias.getValue("Todas")
    ): List<Lugar> {
        val clave = claveApi()
        return ejecutarConsulta(
            categorias = categoriasApi,
            filtro = "circle:$longitud,$latitud,$radio",
            preferencia = "proximity:$longitud,$latitud",
            limite = 40,
            latRef = latitud,
            lonRef = longitud,
            clave = clave
        )
    }

    suspend fun buscarEnElSalvador(
        categoriasApi: String = DatosElSalvador.categorias.getValue("Todas"),
        departamento: String = "Todos"
    ): List<Lugar> {
        val clave = claveApi()
        val dep = DatosElSalvador.departamentos.firstOrNull { it.nombre.equals(departamento, ignoreCase = true) }
        val (filtro, preferencia, latRef, lonRef) = if (dep != null) {
            Quad(
                "circle:${dep.longitud},${dep.latitud},${dep.radioMetros}",
                "proximity:${dep.longitud},${dep.latitud}",
                dep.latitud,
                dep.longitud
            )
        } else {
            Quad("rect:${DatosElSalvador.RECT_PAIS}", null, 13.6929, -89.2182)
        }
        val resultados = ejecutarConsulta(
            categorias = categoriasApi,
            filtro = filtro,
            preferencia = preferencia,
            limite = 50,
            latRef = latRef,
            lonRef = lonRef,
            clave = clave
        )
        return if (dep != null) {
            resultados.map { lugar ->
                if (lugar.departamento == "Sin departamento") lugar.copy(departamento = dep.nombre) else lugar
            }
        } else resultados
    }

    private suspend fun ejecutarConsulta(
        categorias: String,
        filtro: String,
        preferencia: String?,
        limite: Int,
        latRef: Double,
        lonRef: Double,
        clave: String
    ): List<Lugar> {
        val respuesta = ClienteRetrofit.api.consultar(
            categorias = categorias,
            filtro = filtro,
            preferencia = preferencia,
            limite = limite,
            clave = clave
        )
        val elementos = respuesta.getAsJsonArray("features") ?: return emptyList()
        return elementos.mapNotNull { elemento ->
            runCatching {
                val obj = elemento.asJsonObject
                val propiedades = obj.getAsJsonObject("properties") ?: return@runCatching null
                val pais = (propiedades.texto("country_code") ?: propiedades.texto("country") ?: "").lowercase()
                if (pais.isNotBlank() && pais != "sv" && !pais.contains("el salvador")) return@runCatching null
                val nombre = propiedades.texto("name") ?: return@runCatching null
                val id = propiedades.texto("place_id") ?: return@runCatching null
                val direccion = propiedades.texto("formatted") ?: propiedades.texto("address_line2") ?: "Dirección no disponible"
                val categoria = propiedades.categoriaLegible()
                val departamento = normalizarDepartamento(
                    propiedades.texto("state") ?: propiedades.texto("county") ?: propiedades.texto("city")
                        ?: propiedades.texto("district") ?: propiedades.texto("suburb")
                )
                val descripcion = propiedades.texto("description") ?: descripcionPorCategoria(categoria)
                val wikipedia = propiedades.texto("wikipedia") ?: propiedades.texto("wiki_and_media")
                val lat = propiedades.get("lat")?.asDouble ?: latRef
                val lon = propiedades.get("lon")?.asDouble ?: lonRef
                val distancia = propiedades.get("distance")?.takeUnless { it.isJsonNull }?.asDouble?.toInt()
                val horario = propiedades.texto("opening_hours")
                val telefono = propiedades.texto("contact:phone") ?: propiedades.texto("phone") ?: propiedades.texto("datasource:raw:phone")
                val sitioWeb = propiedades.texto("contact:website") ?: propiedades.texto("website") ?: propiedades.texto("datasource:raw:website")
                val calificacion = propiedades.texto("rating") ?: propiedades.texto("rank:popularity")
                Lugar(
                    id = id,
                    nombre = nombre,
                    descripcion = descripcion,
                    direccion = direccion,
                    latitud = lat,
                    longitud = lon,
                    distanciaMetros = distancia,
                    imagen = imagenSitio(lat, lon, clave),
                    wikipedia = wikipedia,
                    categoria = categoria,
                    departamento = departamento,
                    horario = horario,
                    telefono = telefono,
                    sitioWeb = sitioWeb,
                    calificacion = calificacion
                )
            }.getOrNull()
        }.distinctBy { it.id }
    }

    private fun claveApi(): String {
        val clave = BuildConfig.GEOAPIFY_API_KEY
        require(clave.isNotBlank() && clave != "PEGA_TU_API_KEY_AQUI") { "Agrega GEOAPIFY_API_KEY en local.properties" }
        return clave
    }

    suspend fun guardar(lugar: Lugar) = dao.guardar(
        EntidadFavorito(
            lugar.id, lugar.nombre, lugar.descripcion, lugar.direccion,
            lugar.latitud, lugar.longitud, lugar.imagen, lugar.wikipedia,
            lugar.categoria, lugar.departamento, lugar.horario, lugar.telefono,
            lugar.sitioWeb, lugar.calificacion
        )
    )
    suspend fun eliminar(id: String) = dao.eliminar(id)

    suspend fun obtenerGaleria(lugar: Lugar): List<String> {
        val base = listOfNotNull(lugar.imagen)
        val deArticulo = runCatching { fotosDesdeArticulo(lugar) }.getOrDefault(emptyList())
        val deCommons = if (deArticulo.size < 4) {
            runCatching { buscarFotosCommons(lugar) }.getOrDefault(emptyList())
        } else emptyList()
        val fotos = (deArticulo + deCommons).filter { esFotoValida(it) }.distinct().take(10)
        return (fotos + base).distinct().ifEmpty { base }
    }

    private suspend fun fotosDesdeArticulo(lugar: Lugar): List<String> {
        val (idioma, titulo) = resolverWikipedia(lugar) ?: return emptyList()
        val tituloCod = Uri.encode(titulo.replace(' ', '_'))
        val respuesta = ClienteWikipedia.api(idioma).media(tituloCod)
        val items = respuesta.getAsJsonArray("items") ?: return emptyList()
        return items.mapNotNull { item ->
            runCatching {
                val obj = item.asJsonObject
                if (obj.get("type")?.asString != "image") return@runCatching null
                val tituloArchivo = obj.get("title")?.asString.orEmpty()
                if (!esArchivoFoto(tituloArchivo)) return@runCatching null
                if (tituloArchivo.startsWith("File:", ignoreCase = true) || tituloArchivo.startsWith("Archivo:", ignoreCase = true)) {
                    return@runCatching "https://$idioma.wikipedia.org/wiki/Special:FilePath/${Uri.encode(tituloArchivo)}?width=800"
                }
                val src = obj.get("srcset")?.asJsonArray?.lastOrNull()?.asJsonObject?.get("src")?.asString
                    ?: obj.get("src")?.asString
                    ?: obj.get("thumbnail")?.asJsonObject?.get("source")?.asString
                normalizarUrlImagen(src)
            }.getOrNull()
        }.distinct()
    }

    private suspend fun buscarFotosCommons(lugar: Lugar): List<String> {
        val dep = lugar.departamento.takeIf { it != "Sin departamento" }
        val consultas = listOfNotNull(
            "${lugar.nombre} ${dep ?: ""} El Salvador".trim(),
            "${lugar.nombre} El Salvador".trim(),
            lugar.nombre
        ).distinct()
        for (consulta in consultas) {
            val urls = runCatching {
                val respuesta = ClienteWikipedia.commons.buscarImagenesCommons(consulta = consulta)
                val paginas = respuesta.getAsJsonObject("query")?.getAsJsonObject("pages") ?: return@runCatching emptyList<String>()
                paginas.entrySet().mapNotNull { entrada ->
                    runCatching {
                        val pagina = entrada.value.asJsonObject
                        val titulo = pagina.get("title")?.asString.orEmpty()
                        if (!esArchivoFoto(titulo)) return@runCatching null
                        val info = pagina.getAsJsonArray("imageinfo")?.firstOrNull()?.asJsonObject
                        (info?.get("thumburl")?.asString ?: info?.get("url")?.asString)?.let { normalizarUrlImagen(it) }
                    }.getOrNull()
                }
            }.getOrDefault(emptyList())
            if (urls.isNotEmpty()) return urls
        }
        return emptyList()
    }

    private fun esArchivoFoto(titulo: String): Boolean {
        val n = titulo.lowercase()
        if (n.isBlank()) return true
        val bloqueados = listOf(".svg", "icon", "logo", "flag", "coat_of_arms", "escudo", "commons-logo", "wiki letter", "ambox", "crystal", "question", "map of", "locator", "location map", "bandera", "sello")
        return bloqueados.none { n.contains(it) } && listOf(".jpg", ".jpeg", ".png", ".webp", "file:", "archivo:").any { n.contains(it) }
    }

    private fun esFotoValida(url: String): Boolean {
        val u = url.lowercase()
        return u.startsWith("http") && listOf(".svg", "logo", "icon", "flag").none { u.contains(it) }
    }

    private suspend fun resolverWikipedia(lugar: Lugar): Pair<String, String>? {
        val ref = lugar.wikipedia?.trim().orEmpty()
        if (ref.isNotBlank()) {
            val directa = parsearRefWikipedia(ref)
            if (directa != null) return directa
        }
        return runCatching {
            val consulta = listOf(lugar.nombre, lugar.departamento.takeIf { it != "Sin departamento" }, "El Salvador")
                .filterNotNull().joinToString(" ")
            val respuesta = ClienteWikipedia.api("es").buscarPagina(consulta = consulta)
            val titulo = respuesta.getAsJsonObject("query")
                ?.getAsJsonArray("search")
                ?.firstOrNull()?.asJsonObject
                ?.get("title")?.asString
            titulo?.let { "es" to it }
        }.getOrNull()
    }

    private fun parsearRefWikipedia(ref: String): Pair<String, String>? {
        val limpia = ref.substringBefore(';').trim()
        if (limpia.contains("wikipedia.org/wiki/", ignoreCase = true)) {
            val idioma = Regex("https?://([a-z]{2})\\.wikipedia", RegexOption.IGNORE_CASE).find(limpia)?.groupValues?.get(1) ?: "es"
            val titulo = limpia.substringAfter("/wiki/").substringBefore('#').substringBefore('?')
            return if (titulo.isNotBlank()) idioma to titulo.replace('_', ' ') else null
        }
        if (limpia.contains(':')) {
            val partes = limpia.split(':', limit = 2)
            val idioma = partes[0].trim().lowercase().takeIf { it.length == 2 } ?: "es"
            val titulo = partes[1].trim()
            if (titulo.isNotBlank()) return idioma to titulo
        }
        return if (limpia.isNotBlank() && !limpia.startsWith("http")) "es" to limpia else null
    }

    private fun normalizarUrlImagen(src: String?): String? {
        val s = src?.trim().orEmpty()
        if (s.isBlank()) return null
        return when {
            s.startsWith("//") -> "https:$s"
            s.startsWith("http://") -> s.replaceFirst("http://", "https://")
            s.startsWith("https://") -> s
            else -> null
        }
    }
}

private data class Quad(val filtro: String, val preferencia: String?, val lat: Double, val lon: Double)

private fun normalizarDepartamento(valor: String?): String {
    if (valor.isNullOrBlank()) return "Sin departamento"
    val dep = DatosElSalvador.departamentos.firstOrNull {
        valor.equals(it.nombre, ignoreCase = true) || valor.contains(it.nombre, ignoreCase = true) || it.nombre.contains(valor, ignoreCase = true)
    }
    return dep?.nombre ?: valor
}

private fun descripcionPorCategoria(categoria: String): String = when (categoria.lowercase()) {
    "restaurant", "restaurante" -> "Restaurante con opciones para disfrutar comida local o internacional."
    "museum", "museo" -> "Museo o espacio cultural con exhibiciones y valor histórico."
    "park", "parque" -> "Parque ideal para caminar, descansar y disfrutar del entorno."
    "hotel" -> "Alojamiento disponible para visitantes en la zona."
    "cafe", "café" -> "Cafetería para tomar bebidas y pasar un momento agradable."
    "beach", "playa" -> "Playa o zona costera para recreación y turismo."
    else -> "Explora este lugar y descubre lo que ofrece."
}
private fun JsonObject.texto(clave: String): String? = runCatching {
    get(clave)?.takeUnless { it.isJsonNull || !it.isJsonPrimitive }?.asString
}.getOrNull()?.takeIf { it.isNotBlank() }
private fun imagenSitio(latitud: Double, longitud: Double, clave: String): String =
    "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=600&height=400&center=lonlat:$longitud,$latitud&zoom=15&marker=lonlat:$longitud,$latitud;color:%231565c0;size:medium&apiKey=$clave"

private fun JsonObject.categoriaLegible(): String = runCatching {
    val bruto = when {
        get("categories")?.isJsonArray == true -> getAsJsonArray("categories").firstOrNull()?.takeUnless { it.isJsonNull }?.asString
        get("categories")?.isJsonPrimitive == true -> get("categories").asString
        else -> null
    } ?: return@runCatching "Sin categoría"
    bruto.substringAfterLast('.').replace('_', ' ').replaceFirstChar { c ->
        if (c.isLowerCase()) c.titlecase() else c.toString()
    }
}.getOrDefault("Sin categoría")
private fun EntidadFavorito.aLugar() = Lugar(
    id, nombre, descripcion, direccion, latitud, longitud,
    imagen = imagen, wikipedia = wikipedia, categoria = categoria, departamento = departamento,
    horario = horario, telefono = telefono, sitioWeb = sitioWeb, calificacion = calificacion
)
