package ar.uade.cine.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.uade.cine.android.escaner.PantallaPuerta
import ar.uade.cine.android.login.LoginViewModel
import ar.uade.cine.android.login.PantallaLogin

// Sin sesión, el login; con sesión, la Puerta. La sesión vive en el LoginViewModel, solo en memoria.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sin capturas ni miniatura en recientes: se ven la clave y los datos de los clientes.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            Tema {
                val login: LoginViewModel = viewModel()
                val estado by login.estado.collectAsStateWithLifecycle()
                val sesion = estado.sesion
                if (sesion == null) PantallaLogin(login) else PantallaPuerta(sesion, alSalir = login::salir)
            }
        }
    }
}
