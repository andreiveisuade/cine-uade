package ar.uade.cine.android.resultado

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.uade.cine.android.Colores
import ar.uade.cine.android.api.Reserva
import ar.uade.cine.android.escaner.EstadoPuerta

@Composable
fun PantallaResultado(estado: EstadoPuerta, alEscanearOtra: () -> Unit) {
    val contexto = LocalContext.current
    val adelante = estado is EstadoPuerta.Adelante
    val sinVerificar = estado is EstadoPuerta.SinVerificar
    LaunchedEffect(estado) {
        if (adelante) Vibracion.adelante(contexto) else if (!sinVerificar) Vibracion.noPasa(contexto)
    }
    val (titulo, fondo) = when {
        adelante -> "ADELANTE" to Colores.adelante
        sinVerificar -> "NO SE PUDO VERIFICAR" to Colores.sinVerificar
        else -> "NO PASA" to Colores.noPasa
    }
    Column(
        Modifier.fillMaxSize().background(fondo).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(titulo, fontSize = if (sinVerificar) 40.sp else 56.sp, fontWeight = FontWeight.Black, color = Color.White)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (estado) {
                is EstadoPuerta.Adelante -> Detalle(estado.reserva)
                is EstadoPuerta.NoPasa -> Text(estado.mensaje, fontSize = 24.sp, color = Color.White)
                is EstadoPuerta.SinVerificar -> Text(estado.mensaje, fontSize = 24.sp, color = Color.White)
                else -> Unit
            }
        }
        Button(
            alEscanearOtra, Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text(if (sinVerificar) "Reintentar" else "Escanear otra", fontSize = 20.sp) }
    }
}

@Composable
private fun Detalle(reserva: Reserva) {
    Text(reserva.pelicula?.titulo.orEmpty(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text(
        listOfNotNull(reserva.sala?.nombre, Formato.hora(reserva.funcion?.inicio).ifEmpty { null }).joinToString(" · "),
        fontSize = 22.sp, color = Color.White,
    )
    Spacer(Modifier.height(4.dp))
    reserva.entradas.forEach {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(it.codigo, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                Formato.tarifa(it.tarifa) + if (it.requiereAcreditacion) " · pedir carnet" else "",
                fontSize = 22.sp, color = Color.White,
                fontWeight = if (it.requiereAcreditacion) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(Formato.personas(reserva.entradas.size), style = MaterialTheme.typography.titleLarge, color = Color.White)
}
