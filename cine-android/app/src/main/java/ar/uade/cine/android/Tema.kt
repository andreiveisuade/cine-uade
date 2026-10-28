package ar.uade.cine.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Oscuro con el ámbar de la web (tema.js), para que la Puerta se vea del mismo cine.
object Colores {
    val ambar = Color(0xFFF5B301)
    val fondo = Color(0xFF15171D)
    val superficie = Color(0xFF1F222B)
    val adelante = Color(0xFF1E8E4E)
    val noPasa = Color(0xFFC62828)
    val sinVerificar = Color(0xFF455A64)
}

@Composable
fun Tema(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Colores.ambar,
            onPrimary = Colores.fondo,
            background = Colores.fondo,
            surface = Colores.superficie,
        ),
        content = contenido,
    )
}
