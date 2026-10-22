package ar.uade.cine.android.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.uade.cine.android.EsperandoServidor

@Composable
fun PantallaLogin(vm: LoginViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Puerta", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                estado.servidor, vm::cambiarServidor, Modifier.fillMaxWidth(),
                label = { Text("Servidor") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                estado.usuario, vm::cambiarUsuario, Modifier.fillMaxWidth(),
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                estado.clave, vm::cambiarClave, Modifier.fillMaxWidth(),
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { vm.ingresar() }),
            )
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (estado.cargando) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EsperandoServidor() }
            } else {
                Button(vm::ingresar, Modifier.fillMaxWidth()) { Text("Ingresar") }
            }
        }
    }
}
