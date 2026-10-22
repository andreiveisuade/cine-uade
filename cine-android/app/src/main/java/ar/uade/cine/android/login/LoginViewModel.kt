package ar.uade.cine.android.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.uade.cine.android.api.ApiPuerta
import ar.uade.cine.android.api.Rechazo
import ar.uade.cine.android.api.Usuario
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoLogin(
    val servidor: String = LoginViewModel.SERVIDOR_POR_DEFECTO,
    val usuario: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val sesion: Sesion? = null,
)

data class Sesion(val api: ApiPuerta, val usuario: Usuario)

// Valida usuario y clave con POST /sesion; quién puede usar la Puerta lo decide el backend.
class LoginViewModel(private val io: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado

    fun cambiarServidor(valor: String) = _estado.update { it.copy(servidor = valor, error = null) }
    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, error = null) }
    fun cambiarClave(valor: String) = _estado.update { it.copy(clave = valor, error = null) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val api = ApiPuerta(actual.servidor, actual.usuario, actual.clave, io)
                val usuario = api.iniciarSesion()
                _estado.update { it.copy(cargando = false, sesion = Sesion(api, usuario)) }
            } catch (e: Rechazo) {
                _estado.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    fun salir() = _estado.update { it.copy(clave = "", sesion = null) }

    companion object {
        const val SERVIDOR_POR_DEFECTO = "https://cine-uade.onrender.com/api"
    }
}
