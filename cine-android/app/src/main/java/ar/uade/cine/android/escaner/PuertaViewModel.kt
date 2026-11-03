package ar.uade.cine.android.escaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.uade.cine.android.api.ApiPuerta
import ar.uade.cine.android.api.CompraCandy
import ar.uade.cine.android.api.Rechazo
import ar.uade.cine.android.api.Reserva
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Qué canjea el QR: la entrada a la sala o el candy; son independientes y cada uno sirve una vez.
enum class Modo(val titulo: String, val accion: String) {
    ENTRADA("Entrada", "Validar entrada"),
    CANDY("Candy", "Entregar candy");

    companion object {
        fun de(nombre: String?): Modo = entries.firstOrNull { it.name == nombre } ?: ENTRADA
    }
}

sealed interface EstadoPuerta {
    data object Escaneando : EstadoPuerta
    data object Validando : EstadoPuerta
    data class EntradaValida(val reserva: Reserva) : EstadoPuerta
    data class CandyEntregado(val compras: List<CompraCandy>) : EstadoPuerta
    data class Rechazado(val modo: Modo, val mensaje: String) : EstadoPuerta
    data class SinVerificar(val mensaje: String) : EstadoPuerta
}

// Manda cada código al backend en el modo elegido y muestra lo que decide; no reenvía el mismo mientras se ve su resultado.
class PuertaViewModel(
    private val api: ApiPuerta,
    modoInicial: Modo = Modo.ENTRADA,
    private val guardarModo: (Modo) -> Unit = {},
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoPuerta>(EstadoPuerta.Escaneando)
    val estado: StateFlow<EstadoPuerta> = _estado
    private val _modo = MutableStateFlow(modoInicial)
    val modo: StateFlow<Modo> = _modo

    private var ultimoCodigo: String? = null
    private var volvioEn = 0L
    private var vuelta: Job? = null

    // El QR que sigue frente a la cámara se lee enseguida en el otro modo: es el canje que falta.
    fun elegirModo(modo: Modo) {
        _modo.value = modo
        guardarModo(modo)
        ultimoCodigo = null
    }

    // Viaja tal cual: la forma del código la decide el backend.
    fun codigoLeido(codigo: String) {
        if (codigo.isBlank() || _estado.value != EstadoPuerta.Escaneando) return
        // El mismo QR sigue frente a la cámara al volver: se ignora un rato para no rebotarlo.
        if (codigo == ultimoCodigo && reloj() - volvioEn < GRACIA_MS) return
        ultimoCodigo = codigo
        val modo = _modo.value
        _estado.value = EstadoPuerta.Validando
        viewModelScope.launch {
            _estado.value = try {
                when (modo) {
                    Modo.ENTRADA -> EstadoPuerta.EntradaValida(api.registrarAcceso(codigo))
                    Modo.CANDY -> EstadoPuerta.CandyEntregado(api.retirarCandy(codigo))
                }
            } catch (e: Rechazo) {
                // Sin red, timeout o 5xx no se sabe si se canjeó: solo un 4xx es la respuesta del backend.
                if (e.status in 400..499) EstadoPuerta.Rechazado(modo, e.message.orEmpty())
                else EstadoPuerta.SinVerificar(e.message.orEmpty())
            }
            vuelta = launch {
                delay(RESULTADO_MS)
                escanearOtra()
            }
        }
    }

    fun escanearOtra() {
        if (_estado.value is EstadoPuerta.Validando) return
        vuelta?.cancel()
        if (_estado.value is EstadoPuerta.SinVerificar) ultimoCodigo = null
        volvioEn = reloj()
        _estado.value = EstadoPuerta.Escaneando
    }

    companion object {
        const val RESULTADO_MS = 4_000L
        const val GRACIA_MS = 3_000L
    }
}
