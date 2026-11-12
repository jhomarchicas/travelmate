package me.joo.travelmate.vistamodelo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.joo.travelmate.datos.repositorio.RepositorioLugares
import me.joo.travelmate.modelo.DatosElSalvador
import me.joo.travelmate.modelo.Lugar

class ModeloVistaTravelMate(private val repositorio: RepositorioLugares) : ViewModel() {
    private val _lugares = MutableStateFlow<List<Lugar>>(emptyList())
    val lugares = _lugares.asStateFlow()
    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val favoritos = repositorio.favoritos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _seleccionado = MutableStateFlow<Lugar?>(null)
    val seleccionado = _seleccionado.asStateFlow()

    private val _consultaNombre = MutableStateFlow("")
    val consultaNombre = _consultaNombre.asStateFlow()
    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada = _categoriaSeleccionada.asStateFlow()
    private val _departamentoSeleccionado = MutableStateFlow("Todos")
    val departamentoSeleccionado = _departamentoSeleccionado.asStateFlow()
    private val _radioBusqueda = MutableStateFlow(12f)
    val radioBusqueda = _radioBusqueda.asStateFlow()

    val categoriasDisponibles: List<String> = DatosElSalvador.categorias.keys.toList()
    val departamentosDisponibles: List<String> = listOf("Todos") + DatosElSalvador.departamentos.map { it.nombre }

    val lugaresFiltrados: StateFlow<List<Lugar>> = combine(_lugares, _consultaNombre) { lista, consulta ->
        lista.filter { lugar ->
            consulta.isBlank() || lugar.nombre.contains(consulta, ignoreCase = true) ||
                lugar.direccion.contains(consulta, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarConsulta(valor: String) { _consultaNombre.value = valor }
    fun seleccionarCategoria(valor: String) { _categoriaSeleccionada.value = valor }
    fun seleccionarDepartamento(valor: String) { _departamentoSeleccionado.value = valor }
    fun actualizarRadio(valor: Float) { _radioBusqueda.value = valor }
    fun limpiarFiltros() {
        _consultaNombre.value = ""
        _categoriaSeleccionada.value = "Todas"
        _departamentoSeleccionado.value = "Todos"
        _radioBusqueda.value = 12f
    }
    fun seleccionar(lugar: Lugar) {
        _seleccionado.value = lugar
    }

    fun explorarElSalvador() = viewModelScope.launch {
        ejecutar {
            val categoriasApi = DatosElSalvador.categorias[_categoriaSeleccionada.value]
                ?: DatosElSalvador.categorias.getValue("Todas")
            repositorio.buscarEnElSalvador(categoriasApi, _departamentoSeleccionado.value)
        }
    }

    fun buscarCercanos(latitud: Double, longitud: Double) = viewModelScope.launch {
        ejecutar {
            val categoriasApi = DatosElSalvador.categorias[_categoriaSeleccionada.value]
                ?: DatosElSalvador.categorias.getValue("Todas")
            repositorio.buscarCercanos(latitud, longitud, (_radioBusqueda.value * 1000).toInt(), categoriasApi)
        }
    }

    private suspend fun ejecutar(bloque: suspend () -> List<Lugar>) {
        _cargando.value = true
        _error.value = null
        try { _lugares.value = bloque() }
        catch (e: Exception) { _error.value = e.message ?: "No fue posible consultar los lugares" }
        finally { _cargando.value = false }
    }

    fun alternarFavorito(lugar: Lugar) = viewModelScope.launch {
        if (favoritos.value.any { it.id == lugar.id }) repositorio.eliminar(lugar.id) else repositorio.guardar(lugar)
    }
    class Fabrica(private val repositorio: RepositorioLugares) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ModeloVistaTravelMate(repositorio) as T
    }
}
