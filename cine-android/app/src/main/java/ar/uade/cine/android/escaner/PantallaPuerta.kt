package ar.uade.cine.android.escaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ar.uade.cine.android.Colores
import ar.uade.cine.android.EsperandoServidor
import ar.uade.cine.android.login.Sesion
import ar.uade.cine.android.resultado.PantallaResultado

// La cámara queda prendida debajo del resultado para no tardar en volver a leer.
@Composable
fun PantallaPuerta(sesion: Sesion, alSalir: () -> Unit) {
    val vm: PuertaViewModel = viewModel(factory = viewModelFactory { initializer { PuertaViewModel(sesion.api) } })
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var permitida by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var preguntada by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitida = it
        preguntada = true
    }
    LaunchedEffect(Unit) { if (!permitida) pedirPermiso.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Colores.fondo)) {
        if (permitida) {
            Camara(vm::codigoLeido, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.Center).size(260.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            )
        } else if (preguntada) {
            PermisoNegado(
                Modifier.align(Alignment.Center),
                alReintentar = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                alAbrirAjustes = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                    )
                },
            )
        }
        Barra(sesion, vm::codigoLeido, alSalir, Modifier.align(Alignment.BottomCenter))

        when (val actual = estado) {
            is EstadoPuerta.Validando -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) { EsperandoServidor() }
            is EstadoPuerta.Adelante, is EstadoPuerta.NoPasa, is EstadoPuerta.SinVerificar -> PantallaResultado(actual, vm::escanearOtra)
            EstadoPuerta.Escaneando -> Unit
        }
    }
}

@Composable
private fun PermisoNegado(modifier: Modifier, alReintentar: () -> Unit, alAbrirAjustes: () -> Unit) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Sin la cámara no se pueden leer los QR. Podés darle permiso o escribir el código abajo.",
            textAlign = TextAlign.Center,
        )
        Button(alReintentar) { Text("Dar permiso") }
        TextButton(alAbrirAjustes) { Text("Abrir ajustes") }
    }
}

// Ingreso a mano por si la cámara no enfoca o el ticket está gastado.
@Composable
private fun Barra(sesion: Sesion, alIngresar: (String) -> Unit, alSalir: () -> Unit, modifier: Modifier) {
    var codigo by remember { mutableStateOf("") }
    val enviar = {
        alIngresar(codigo)
        codigo = ""
    }
    Column(
        modifier.fillMaxWidth().background(Colores.fondo.copy(alpha = 0.85f)).safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                codigo, { codigo = it }, Modifier.weight(1f),
                placeholder = { Text("Código del ticket") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { enviar() }),
            )
            Button(enviar, enabled = codigo.isNotBlank()) { Text("Validar") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sesion.usuario.nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(alSalir) { Text("Salir") }
        }
    }
}
