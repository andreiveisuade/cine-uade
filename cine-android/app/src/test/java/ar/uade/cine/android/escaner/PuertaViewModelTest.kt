package ar.uade.cine.android.escaner

import ar.uade.cine.android.api.ApiPuerta
import ar.uade.cine.android.api.Ejemplos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
class PuertaViewModelTest {
    private val despachador = StandardTestDispatcher()
    private lateinit var servidor: MockWebServer
    private var ahora = 0L

    @BeforeEach
    fun levantar() {
        Dispatchers.setMain(despachador)
        servidor = MockWebServer()
        servidor.start()
    }

    @AfterEach
    fun apagar() {
        servidor.shutdown()
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        PuertaViewModel(ApiPuerta(servidor.url("/api").toString(), "p", "c", despachador)) { ahora }

    private fun TestScope.leer(vm: PuertaViewModel, codigo: String) {
        vm.codigoLeido(codigo)
        runCurrent()
    }

    @Test
    fun conUn200DiceAdelanteConLaReserva() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val vm = viewModel()

        leer(vm, "K7M2 P9XQ")

        val estado = vm.estado.value as EstadoPuerta.Adelante
        assertEquals("Hablan las aves", estado.reserva.pelicula?.titulo)
        assertEquals("""{"codigo":"K7M2 P9XQ"}""", servidor.takeRequest().body.readUtf8())
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "ya usada | 400 | {\"error\":\"La entrada ya se usó\"} | La entrada ya se usó",
            "credenciales | 401 | {\"error\":\"Email o contraseña incorrectos\"} | Email o contraseña incorrectos",
            "no existe | 404 | {\"error\":\"No existe la reserva K7M2P9XQ\"} | No existe la reserva K7M2P9XQ",
            "demasiados intentos | 429 | {\"error\":\"Demasiados intentos\"} | Demasiados intentos",
            "html | 502 | <html>Bad Gateway</html> | El servidor no está disponible (HTTP 502): volvé a intentarlo en un rato",
        ],
    )
    fun conUnErrorDiceNoPasaConElMensaje(caso: String, status: Int, cuerpo: String, mensaje: String) = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(status).setBody(cuerpo))
        val vm = viewModel()

        leer(vm, "K7M2P9XQ")

        assertEquals(EstadoPuerta.NoPasa(mensaje), vm.estado.value)
    }

    @Test
    fun mientrasMuestraElResultadoNoReenviaNada() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val vm = viewModel()

        leer(vm, "K7M2P9XQ")
        leer(vm, "K7M2P9XQ")
        leer(vm, "ABCDEFGH")

        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun vuelveSoloAlEscanerYNoRebotaElMismoCodigo() = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Ya se usó"}"""))
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val vm = viewModel()
        leer(vm, "K7M2P9XQ")

        advanceTimeBy(PuertaViewModel.RESULTADO_MS + 1)
        assertEquals(EstadoPuerta.Escaneando, vm.estado.value)

        leer(vm, "K7M2P9XQ")
        assertEquals(EstadoPuerta.Escaneando, vm.estado.value)

        ahora += PuertaViewModel.GRACIA_MS
        leer(vm, "K7M2P9XQ")
        assertInstanceOf(EstadoPuerta.Adelante::class.java, vm.estado.value)
    }
}
