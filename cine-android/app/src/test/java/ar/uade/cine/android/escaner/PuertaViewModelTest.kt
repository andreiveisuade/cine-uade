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
import okhttp3.mockwebserver.SocketPolicy
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

    private val guardados = mutableListOf<Modo>()

    private fun viewModel(modo: Modo = Modo.ENTRADA) =
        PuertaViewModel(ApiPuerta(servidor.url("/api").toString(), "p", "c", despachador), modo, guardados::add) { ahora }

    private fun TestScope.leer(vm: PuertaViewModel, codigo: String) {
        vm.codigoLeido(codigo)
        runCurrent()
    }

    @Test
    fun conUn200DiceEntradaValidaConLaReserva() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val vm = viewModel()

        leer(vm, "K7M2 P9XQ")

        val estado = vm.estado.value as EstadoPuerta.EntradaValida
        assertEquals("Hablan las aves", estado.reserva.pelicula?.titulo)
        val pedido = servidor.takeRequest()
        assertEquals("/api/acceso", pedido.path)
        assertEquals("""{"codigo":"K7M2 P9XQ"}""", pedido.body.readUtf8())
    }

    @Test
    fun enModoCandyEntregaElCandyYNoRegistraElIngreso() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody(Ejemplos.CANDY_RETIRADO))
        val vm = viewModel(Modo.CANDY)

        leer(vm, "K7M2P9XQ")

        val estado = vm.estado.value as EstadoPuerta.CandyEntregado
        assertEquals(listOf("Agua", "Combo clásico"), estado.compras.single().items.map { it.nombre })
        assertEquals("/api/candy/retiros", servidor.takeRequest().path)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun elCandyYaRetiradoSeRechazaEnModoCandy() = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":"El candy de esa entrada ya se retiró el 21/10 19:50"}"""))
        val vm = viewModel(Modo.CANDY)

        leer(vm, "K7M2P9XQ")

        assertEquals(EstadoPuerta.Rechazado(Modo.CANDY, "El candy de esa entrada ya se retiró el 21/10 19:50"), vm.estado.value)
    }

    @Test
    fun cambiarDeModoLoGuardaYLeeEnSeguidaElMismoQr() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        servidor.enqueue(MockResponse().setBody(Ejemplos.CANDY_RETIRADO))
        val vm = viewModel()
        leer(vm, "K7M2P9XQ")
        vm.escanearOtra()

        vm.elegirModo(Modo.CANDY)
        leer(vm, "K7M2P9XQ")

        assertEquals(Modo.CANDY, vm.modo.value)
        assertEquals(listOf(Modo.CANDY), guardados)
        assertInstanceOf(EstadoPuerta.CandyEntregado::class.java, vm.estado.value)
        assertEquals(listOf("/api/acceso", "/api/candy/retiros"), List(2) { servidor.takeRequest().path })
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource("guardado, CANDY, CANDY", "nunca elegido, , ENTRADA", "valor viejo, OTRO, ENTRADA")
    fun elModoGuardadoSeLeeOArrancaEnEntrada(caso: String, guardado: String?, esperado: Modo) {
        assertEquals(esperado, Modo.de(guardado))
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "ya usada | 400 | {\"error\":\"La entrada ya se usó\"} | La entrada ya se usó",
            "credenciales | 401 | {\"error\":\"Email o contraseña incorrectos\"} | Email o contraseña incorrectos",
            "no existe | 404 | {\"error\":\"No existe la reserva K7M2P9XQ\"} | No existe la reserva K7M2P9XQ",
            "demasiados intentos | 429 | {\"error\":\"Demasiados intentos\"} | Demasiados intentos",
        ],
    )
    fun conUnErrorRechazaLaEntradaConElMensaje(caso: String, status: Int, cuerpo: String, mensaje: String) = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(status).setBody(cuerpo))
        val vm = viewModel()

        leer(vm, "K7M2P9XQ")

        assertEquals(EstadoPuerta.Rechazado(Modo.ENTRADA, mensaje), vm.estado.value)
    }

    @Test
    fun conElServidorCaidoNoDiceNoPasa() = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(503).setBody("<html>Service Unavailable</html>"))
        val vm = viewModel()

        leer(vm, "K7M2P9XQ")

        assertEquals(
            EstadoPuerta.SinVerificar("El servidor no está disponible (HTTP 503): volvé a intentarlo en un rato"),
            vm.estado.value,
        )
    }

    @Test
    fun sinRespuestaNoDiceNoPasaYSePuedeReintentarEnSeguida() = runTest(despachador) {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val vm = viewModel()

        leer(vm, "K7M2P9XQ")
        assertInstanceOf(EstadoPuerta.SinVerificar::class.java, vm.estado.value)

        vm.escanearOtra()
        leer(vm, "K7M2P9XQ")
        assertInstanceOf(EstadoPuerta.EntradaValida::class.java, vm.estado.value)
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
        assertInstanceOf(EstadoPuerta.EntradaValida::class.java, vm.estado.value)
    }
}
