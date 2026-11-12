package me.joo.travelmate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import me.joo.travelmate.interfaz.componentes.TarjetaLugar
import me.joo.travelmate.interfaz.tema.TemaTravelMate
import me.joo.travelmate.modelo.Lugar
import me.joo.travelmate.ubicacion.GestorUbicacion
import me.joo.travelmate.vistamodelo.ModeloVistaTravelMate
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repositorio = (application as AplicacionTravelMate).repositorio
        setContent {
            TemaTravelMate {
                val modelo: ModeloVistaTravelMate = viewModel(factory = ModeloVistaTravelMate.Fabrica(repositorio))
                PantallaPrincipal(modelo)
            }
        }
    }
}

private const val PREFS_APP = "travelmate_prefs"
private const val PREF_ONBOARDING = "onboarding_completado"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(modelo: ModeloVistaTravelMate) {
    val contexto = LocalContext.current
    val prefs = remember { contexto.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE) }
    var onboardingCompletado by remember { mutableStateOf(prefs.getBoolean(PREF_ONBOARDING, false)) }

    if (!onboardingCompletado) {
        PantallaOnboarding(
            alFinalizar = {
                prefs.edit().putBoolean(PREF_ONBOARDING, true).apply()
                onboardingCompletado = true
            }
        )
        return
    }

    val lugaresBase by modelo.lugares.collectAsState()
    val lugares by modelo.lugaresFiltrados.collectAsState()
    val favoritos by modelo.favoritos.collectAsState()
    val cargando by modelo.cargando.collectAsState()
    val error by modelo.error.collectAsState()
    val seleccionado by modelo.seleccionado.collectAsState()
    val consultaNombre by modelo.consultaNombre.collectAsState()
    val categoriaSeleccionada by modelo.categoriaSeleccionada.collectAsState()
    val departamentoSeleccionado by modelo.departamentoSeleccionado.collectAsState()
    val radioBusqueda by modelo.radioBusqueda.collectAsState()
    val categorias = modelo.categoriasDisponibles
    val departamentos = modelo.departamentosDisponibles

    var pestana by remember { mutableIntStateOf(0) }
    var mostrarDetalle by remember { mutableStateOf(false) }
    var errorUbicacion by remember { mutableStateOf(false) }
    var mostrarHojaFiltros by remember { mutableStateOf(false) }
    var consultaFavoritos by remember { mutableStateOf("") }
    val hojaFiltrosState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(enabled = mostrarDetalle) { mostrarDetalle = false }

    fun cargarCercanos() {
        GestorUbicacion.obtener(contexto) { lat, lon ->
            if (lat != null && lon != null) {
                errorUbicacion = false
                modelo.buscarCercanos(lat, lon)
            } else errorUbicacion = true
        }
    }

    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) cargarCercanos() else errorUbicacion = true
    }

    fun solicitarUbicacion() {
        val concedido = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
            ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
        }
        if (concedido) cargarCercanos() else permisos.launch(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }

    val filtrosActivos = listOfNotNull(
        if (consultaNombre.isNotBlank()) "Búsqueda" else null,
        if (categoriaSeleccionada != "Todas") categoriaSeleccionada else null,
        if (departamentoSeleccionado != "Todos") departamentoSeleccionado else null,
        if (pestana == 1 && radioBusqueda.toInt() != 12) "${radioBusqueda.toInt()} km" else null
    ).size

    if (mostrarHojaFiltros) {
        ModalBottomSheet(
            onDismissRequest = { mostrarHojaFiltros = false },
            sheetState = hojaFiltrosState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            HojaFiltros(
                pestana = pestana,
                consultaNombre = consultaNombre,
                categoriaSeleccionada = categoriaSeleccionada,
                departamentoSeleccionado = departamentoSeleccionado,
                categorias = categorias,
                departamentos = departamentos,
                radioBusqueda = radioBusqueda,
                alCambiarConsulta = modelo::actualizarConsulta,
                alSeleccionarCategoria = modelo::seleccionarCategoria,
                alSeleccionarDepartamento = modelo::seleccionarDepartamento,
                alCambiarRadio = modelo::actualizarRadio,
                alLimpiar = modelo::limpiarFiltros,
                alCerrar = { mostrarHojaFiltros = false }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (mostrarDetalle) "Detalles del destino" else "TravelMate",
                            fontWeight = FontWeight.Bold
                        )
                        if (!mostrarDetalle) {
                            Text(
                                when (pestana) {
                                    0 -> "Explora El Salvador"
                                    1 -> "Cerca de ti"
                                    2 -> "Tus favoritos"
                                    else -> "Sobre la app"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (mostrarDetalle) {
                        IconButton(onClick = { mostrarDetalle = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!mostrarDetalle) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    listOf(
                        Triple("Explorar", Icons.Outlined.Explore, 0),
                        Triple("Cercanos", Icons.Outlined.NearMe, 1),
                        Triple("Favoritos", Icons.Outlined.FavoriteBorder, 2),
                        Triple("Creadores", Icons.Outlined.Groups, 3)
                    ).forEach { (titulo, icono, indice) ->
                        NavigationBarItem(
                            selected = pestana == indice,
                            onClick = { pestana = indice },
                            icon = { Icon(icono, titulo) },
                            label = { Text(titulo) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { espacio ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(espacio)
        ) {
            when {
                mostrarDetalle -> {
                    seleccionado?.let { lugar ->
                        DetalleLugar(
                            lugar = lugar,
                            favorito = favoritos.any { it.id == lugar.id },
                            alternar = { modelo.alternarFavorito(lugar) },
                            abrirGoogleMaps = { abrirMapa(contexto, lugar, "google") },
                            abrirWaze = { abrirMapa(contexto, lugar, "waze") },
                            comoLlegar = { abrirNavegacion(contexto, lugar) }
                        )
                    }
                }
                pestana == 2 -> {
                    PantallaFavoritos(
                        favoritos = favoritos,
                        consulta = consultaFavoritos,
                        alCambiarConsulta = { consultaFavoritos = it },
                        alAbrir = { lugar -> modelo.seleccionar(lugar); mostrarDetalle = true },
                        alFavorito = { modelo.alternarFavorito(it) }
                    )
                }
                pestana == 3 -> {
                    PantallaCreadores()
                }
                else -> {
                    PantallaExplorar(
                        pestana = pestana,
                        lugaresBase = lugaresBase,
                        lugares = lugares,
                        favoritos = favoritos,
                        cargando = cargando,
                        error = error,
                        errorUbicacion = errorUbicacion,
                        consultaNombre = consultaNombre,
                        categoriaSeleccionada = categoriaSeleccionada,
                        departamentoSeleccionado = departamentoSeleccionado,
                        radioBusqueda = radioBusqueda,
                        filtrosActivos = filtrosActivos,
                        alCambiarConsulta = modelo::actualizarConsulta,
                        alAbrirFiltros = { mostrarHojaFiltros = true },
                        alBuscar = { if (pestana == 0) modelo.explorarElSalvador() else solicitarUbicacion() },
                        alAbrir = { lugar -> modelo.seleccionar(lugar); mostrarDetalle = true },
                        alFavorito = { modelo.alternarFavorito(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaOnboarding(alFinalizar: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { espacio ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacio)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.TravelExplore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }
            Text("Bienvenido a TravelMate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Descubre destinos en El Salvador, guarda tus favoritos y encuentra lugares cerca de ti.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilaOnboarding(
                        icono = Icons.Outlined.Explore,
                        titulo = "Explorar",
                        texto = "Busca destinos por nombre, categoría o departamento en todo El Salvador usando filtros."
                    )
                    FilaOnboarding(
                        icono = Icons.Outlined.NearMe,
                        titulo = "Cercanos",
                        texto = "Encuentra lugares alrededor de tu ubicación y ajusta el radio de búsqueda."
                    )
                    FilaOnboarding(
                        icono = Icons.Outlined.MyLocation,
                        titulo = "Permiso de ubicación",
                        texto = "La app pedirá acceso a tu ubicación solo cuando uses \"Cercanos\" para mostrar sitios cerca de ti. Puedes seguir usando Explorar sin concederlo."
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = alFinalizar,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Comenzar", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun FilaOnboarding(icono: ImageVector, titulo: String, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PantallaExplorar(
    pestana: Int,
    lugaresBase: List<Lugar>,
    lugares: List<Lugar>,
    favoritos: List<Lugar>,
    cargando: Boolean,
    error: String?,
    errorUbicacion: Boolean,
    consultaNombre: String,
    categoriaSeleccionada: String,
    departamentoSeleccionado: String,
    radioBusqueda: Float,
    filtrosActivos: Int,
    alCambiarConsulta: (String) -> Unit,
    alAbrirFiltros: () -> Unit,
    alBuscar: () -> Unit,
    alAbrir: (Lugar) -> Unit,
    alFavorito: (Lugar) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (pestana == 0) "Descubre nuevos destinos" else "Encuentra sitios a tu alrededor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (pestana == 0) "Filtra por nombre, categoría o departamento para explorar todo el país."
                        else "Activa tu ubicación y ajusta el radio para ver opciones cercanas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = consultaNombre,
                onValueChange = alCambiarConsulta,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o dirección") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (consultaNombre.isNotBlank()) {
                        IconButton(onClick = { alCambiarConsulta("") }) { Icon(Icons.Outlined.Close, "Limpiar búsqueda") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = alAbrirFiltros,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (filtrosActivos > 0) "Filtros ($filtrosActivos)" else "Filtros")
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    if (categoriaSeleccionada != "Todas") item { ResumenChip(categoriaSeleccionada) }
                    if (departamentoSeleccionado != "Todos") item { ResumenChip(departamentoSeleccionado) }
                    if (pestana == 1) item { ResumenChip("${radioBusqueda.toInt()} km") }
                }
            }
        }
        item {
            Button(
                onClick = alBuscar,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (pestana == 0) Icons.Outlined.TravelExplore else Icons.Outlined.MyLocation, null)
                Spacer(Modifier.width(8.dp))
                Text(if (pestana == 0) "Buscar destinos" else "Buscar lugares cercanos", fontWeight = FontWeight.SemiBold)
            }
        }
        if (errorUbicacion && pestana == 1) {
            item {
                MensajeEstado(
                    icono = Icons.Outlined.LocationOff,
                    texto = "No fue posible obtener tu ubicación. Activa el GPS y concede el permiso.",
                    error = true
                )
            }
        }
        if (cargando) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) }
        }
        error?.let {
            item { MensajeEstado(icono = Icons.Outlined.ErrorOutline, texto = it, error = true) }
        }
        if (lugares.isEmpty() && !cargando && error == null) {
            item {
                EstadoVacio(
                    icono = if (lugaresBase.isEmpty()) Icons.Outlined.Map else Icons.Outlined.SearchOff,
                    titulo = if (lugaresBase.isEmpty()) "Listo para explorar" else "Sin resultados",
                    texto = if (lugaresBase.isEmpty())
                        "Pulsa el botón de búsqueda para descubrir destinos increíbles."
                    else
                        "Prueba cambiando los filtros o la búsqueda para ver más lugares."
                )
            }
        } else {
            items(lugares, key = { it.id }) { lugar ->
                TarjetaLugar(
                    lugar = lugar,
                    esFavorito = favoritos.any { it.id == lugar.id },
                    alAbrir = { alAbrir(lugar) },
                    alFavorito = { alFavorito(lugar) }
                )
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
fun PantallaFavoritos(
    favoritos: List<Lugar>,
    consulta: String,
    alCambiarConsulta: (String) -> Unit,
    alAbrir: (Lugar) -> Unit,
    alFavorito: (Lugar) -> Unit
) {
    val filtrados = favoritos.filter { consulta.isBlank() || it.nombre.contains(consulta, ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = consulta,
                onValueChange = alCambiarConsulta,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar en favoritos") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (consulta.isNotBlank()) {
                        IconButton(onClick = { alCambiarConsulta("") }) { Icon(Icons.Outlined.Close, "Limpiar") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (filtrados.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = if (favoritos.isEmpty()) "Aún no tienes favoritos" else "Sin coincidencias",
                    texto = if (favoritos.isEmpty())
                        "Guarda los destinos que te gusten desde Explorar para verlos aquí."
                    else
                        "No hay favoritos que coincidan con tu búsqueda."
                )
            }
        } else {
            items(filtrados, key = { it.id }) { lugar ->
                TarjetaLugar(lugar, true, { alAbrir(lugar) }, { alFavorito(lugar) })
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

data class Creador(val nombre: String, val archivoImagen: String)

@Composable
fun PantallaCreadores() {
    val creadores = listOf(
        Creador("Jhomar Caleb Chicas De Paz", "jhomar.png"),
        Creador("Owen Alexander Torres Muñoz", "owen.png"),
        Creador("Rigoberto Antonio Pineda Arias", "rigoberto.png")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Text("Creadores", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Equipo detrás del desarrollo de TravelMate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(creadores) { creador ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = "file:///android_asset/creadores/${creador.archivoImagen}",
                        contentDescription = creador.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        loading = { PlaceholderCreador() },
                        error = { PlaceholderCreador() }
                    )
                    Text(creador.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun PlaceholderCreador() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun ResumenChip(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MensajeEstado(icono: ImageVector, texto: String, error: Boolean) {
    val bg = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icono, null, tint = fg)
        Text(texto, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaFiltros(
    pestana: Int,
    consultaNombre: String,
    categoriaSeleccionada: String,
    departamentoSeleccionado: String,
    categorias: List<String>,
    departamentos: List<String>,
    radioBusqueda: Float,
    alCambiarConsulta: (String) -> Unit,
    alSeleccionarCategoria: (String) -> Unit,
    alSeleccionarDepartamento: (String) -> Unit,
    alCambiarRadio: (Float) -> Unit,
    alLimpiar: () -> Unit,
    alCerrar: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Filtros de búsqueda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = alLimpiar) { Text("Limpiar todo") }
        }

        OutlinedTextField(
            value = consultaNombre,
            onValueChange = alCambiarConsulta,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre o dirección") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorias) { categoria ->
                    FilterChip(
                        selected = categoriaSeleccionada == categoria,
                        onClick = { alSeleccionarCategoria(categoria) },
                        label = { Text(categoria) },
                        leadingIcon = if (categoriaSeleccionada == categoria) {
                            { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        SelectorFiltro("Departamento", departamentos, departamentoSeleccionado, alSeleccionarDepartamento)

        if (pestana == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Radio de búsqueda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${radioBusqueda.toInt()} km", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = radioBusqueda, onValueChange = alCambiarRadio, valueRange = 1f..50f, steps = 48)
                Text("Busca lugares dentro de este rango desde tu ubicación.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (departamentoSeleccionado == "Todos") "La búsqueda se realizará en todo El Salvador."
                        else "Buscando destinos en: $departamentoSeleccionado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = alCerrar,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Aplicar filtros", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFiltro(titulo: String, opciones: List<String>, seleccion: String, alSeleccionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = seleccion,
            onValueChange = {},
            readOnly = true,
            label = { Text(titulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(text = { Text(opcion) }, onClick = { alSeleccionar(opcion); expandido = false })
            }
        }
    }
}

fun abrirMapa(contexto: Context, lugar: Lugar, app: String) {
    val lat = lugar.latitud
    val lon = lugar.longitud
    val nombre = Uri.encode(lugar.nombre)
    val uri = when (app) {
        "waze" -> Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes")
        else -> Uri.parse("geo:$lat,$lon?q=$lat,$lon($nombre)")
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        if (app == "google") setPackage("com.google.android.apps.maps")
    }
    runCatching { contexto.startActivity(intent) }.onFailure {
        val web = when (app) {
            "waze" -> Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes")
            else -> Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
        }
        runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }
}

fun abrirNavegacion(contexto: Context, lugar: Lugar) {
    val lat = lugar.latitud
    val lon = lugar.longitud
    val uri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    runCatching { contexto.startActivity(intent) }.onFailure {
        val web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")
        runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }
}

@Composable
fun DetalleLugar(
    lugar: Lugar,
    favorito: Boolean,
    alternar: () -> Unit,
    abrirGoogleMaps: () -> Unit,
    abrirWaze: () -> Unit,
    comoLlegar: () -> Unit
) {
    val contexto = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ubicación en el mapa", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(320.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            MapaInteractivo(lugar = lugar)
        }
        Text("Puedes mover, acercar y alejar el mapa para explorar la zona.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(lugar.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lugar.categoria.isNotBlank() && lugar.categoria != "Sin categoría") {
                AssistChip(onClick = {}, label = { Text(lugar.categoria) }, leadingIcon = { Icon(Icons.Outlined.Category, null) })
            }
            if (lugar.departamento.isNotBlank() && lugar.departamento != "Sin departamento") {
                AssistChip(onClick = {}, label = { Text(lugar.departamento) }, leadingIcon = { Icon(Icons.Outlined.Place, null) })
            }
        }

        Button(
            onClick = comoLlegar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Directions, null)
            Spacer(Modifier.width(8.dp))
            Text("Cómo llegar", fontWeight = FontWeight.SemiBold)
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Información del destino", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (lugar.direccion.isNotBlank()) {
                    FilaDetalle(Icons.Outlined.LocationOn, "Dirección", lugar.direccion)
                }
                if (lugar.descripcion.isNotBlank()) {
                    FilaDetalle(Icons.Outlined.Info, "Descripción", lugar.descripcion)
                }
                lugar.horario?.takeIf { it.isNotBlank() }?.let {
                    FilaDetalle(Icons.Outlined.Schedule, "Horario", it)
                }
                lugar.telefono?.takeIf { it.isNotBlank() }?.let { tel ->
                    FilaDetalleAccion(
                        icono = Icons.Outlined.Phone,
                        etiqueta = "Teléfono",
                        valor = tel,
                        accionTexto = "Llamar",
                        alAccionar = {
                            runCatching { contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) }
                        }
                    )
                }
                lugar.sitioWeb?.takeIf { it.isNotBlank() }?.let { web ->
                    val url = if (web.startsWith("http://") || web.startsWith("https://")) web else "https://$web"
                    FilaDetalleAccion(
                        icono = Icons.Outlined.Language,
                        etiqueta = "Sitio web",
                        valor = web,
                        accionTexto = "Abrir",
                        alAccionar = {
                            runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }
                    )
                }
                lugar.calificacion?.takeIf { it.isNotBlank() }?.let {
                    FilaDetalle(Icons.Outlined.Star, "Calificación", it)
                }
                lugar.distanciaMetros?.let {
                    val texto = if (it >= 1000) "%.1f km".format(it / 1000.0) else "$it m"
                    FilaDetalle(Icons.Outlined.Straighten, "Distancia aproximada", texto)
                }
                FilaDetalle(Icons.Outlined.MyLocation, "Coordenadas", "${"%.5f".format(lugar.latitud)}, ${"%.5f".format(lugar.longitud)}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = abrirGoogleMaps, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Map, null)
                Spacer(Modifier.width(6.dp))
                Text("Google Maps")
            }
            Button(onClick = abrirWaze, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Navigation, null)
                Spacer(Modifier.width(6.dp))
                Text("Waze")
            }
        }
        Button(onClick = alternar, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(if (favorito) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, null)
            Spacer(Modifier.width(8.dp))
            Text(if (favorito) "Quitar de favoritos" else "Guardar en favoritos", fontWeight = FontWeight.SemiBold)
        }
        lugar.wikipedia?.let {
            val ref = if (it.startsWith("http")) it else "Wikipedia: $it"
            Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun FilaDetalle(icono: ImageVector, etiqueta: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun FilaDetalleAccion(icono: ImageVector, etiqueta: String, valor: String, accionTexto: String, alAccionar: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = alAccionar, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(accionTexto) }
    }
}

@Composable
fun MapaInteractivo(lugar: Lugar) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val punto = remember(lugar.id, lugar.latitud, lugar.longitud) { GeoPoint(lugar.latitud, lugar.longitud) }
    val mapView = remember {
        MapView(contexto).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            setOnTouchListener { v, event ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(punto, lugar.nombre) {
        mapView.controller.setCenter(punto)
        mapView.overlays.clear()
        val marcador = Marker(mapView).apply {
            position = punto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = lugar.nombre
            subDescription = lugar.direccion
        }
        mapView.overlays.add(marcador)
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}
