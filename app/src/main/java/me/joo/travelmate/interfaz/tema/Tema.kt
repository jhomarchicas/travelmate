package me.joo.travelmate.interfaz.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AzulPrincipal = Color(0xFF0E7490)
private val AzulSecundario = Color(0xFF0284C7)
private val AzulClaro = Color(0xFFCFFAFE)
private val AzulOscuro = Color(0xFF164E63)
private val Fondo = Color(0xFFF3F7FA)
private val Superficie = Color(0xFFFFFFFF)
private val SuperficieVariante = Color(0xFFE6EEF5)

private val colores = lightColorScheme(
    primary = AzulPrincipal,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulOscuro,
    secondary = AzulSecundario,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEFFC),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF14B8A6),
    onTertiary = Color.White,
    background = Fondo,
    onBackground = Color(0xFF102A43),
    surface = Superficie,
    onSurface = Color(0xFF102A43),
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = Color(0xFF486581),
    outline = Color(0xFFB6C9D8),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun TemaTravelMate(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, content = contenido)
}
