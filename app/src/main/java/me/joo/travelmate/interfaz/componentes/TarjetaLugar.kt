package me.joo.travelmate.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import me.joo.travelmate.modelo.Lugar

@Composable
fun TarjetaLugar(lugar: Lugar, esFavorito: Boolean, alAbrir: () -> Unit, alFavorito: () -> Unit) {
    Card(
        onClick = alAbrir,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                if (lugar.imagen != null) {
                    SubcomposeAsyncImage(
                        model = lugar.imagen,
                        contentDescription = lugar.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { ImagenAlternativa() },
                        error = { ImagenAlternativa() }
                    )
                } else {
                    ImagenAlternativa()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )
                IconButton(
                    onClick = alFavorito,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.92f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (esFavorito) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                    )
                }
                lugar.distanciaMetros?.let {
                    val texto = if (it >= 1000) "%.1f km".format(it / 1000.0) else "$it m"
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            texto,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lugar.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (lugar.direccion.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            lugar.direccion,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lugar.categoria.isNotBlank() && lugar.categoria != "Sin categoría") {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(lugar.categoria, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    if (lugar.departamento.isNotBlank() && lugar.departamento != "Sin departamento") {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(lugar.departamento, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImagenAlternativa() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Landscape,
            contentDescription = "Imagen no disponible",
            modifier = Modifier.size(54.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    }
}
