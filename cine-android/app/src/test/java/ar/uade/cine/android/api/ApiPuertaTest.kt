package ar.uade.cine.android.api

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ApiPuertaTest {
    private lateinit var servidor: MockWebServer

    @BeforeEach
    fun levantar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @AfterEach
    fun apagar() = servidor.shutdown()

    @Test
    fun registrarAccesoMandaElCodigoConBasicYLeeLaReserva() = runTest {
        servidor.enqueue(MockResponse().setBody(Ejemplos.RESERVA))
        val reserva = ApiPuerta(servidor.url("/api/").toString(), " puerta ", "clave", StandardTestDispatcher(testScheduler))
            .registrarAcceso("K7M2P9XQ")

        val pedido = servidor.takeRequest()
        assertEquals("/api/acceso", pedido.path)
        assertEquals("POST", pedido.method)
        assertEquals(Credentials.basic("puerta", "clave"), pedido.getHeader("Authorization"))
        assertEquals("""{"codigo":"K7M2P9XQ"}""", pedido.body.readUtf8())
        assertEquals("Hablan las aves", reserva.pelicula?.titulo)
        assertEquals("Sala 2", reserva.sala?.nombre)
        assertEquals("2026-10-22T20:30:00", reserva.funcion?.inicio)
        assertEquals(listOf(Entrada("C5", "GENERAL"), Entrada("C6", "JUBILADO")), reserva.entradas)
        assertEquals("2026-10-22T20:12:00", reserva.ingresadaEn)
    }

    @Test
    fun iniciarSesionLeeElUsuario() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id":3,"nombre":"Puerta","email":"p@x","rol":"ACOMODADOR"}"""))
        val usuario = ApiPuerta(servidor.url("/api").toString(), "p@x", "c", StandardTestDispatcher(testScheduler))
            .iniciarSesion()

        assertEquals("/api/sesion", servidor.takeRequest().path)
        assertEquals("ACOMODADOR", usuario.rol)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "sin pagar | 400 | {\"error\":\"La reserva no está pagada\"} | La reserva no está pagada",
            "credenciales | 401 | {\"error\":\"Email o contraseña incorrectos\"} | Email o contraseña incorrectos",
            "no existe | 404 | {\"error\":\"No existe la reserva K7M2P9XQ\"} | No existe la reserva K7M2P9XQ",
            "demasiados intentos | 429 | {\"error\":\"Demasiados intentos\"} | Demasiados intentos",
            "html de un proxy | 502 | <html><body>Bad Gateway</body></html> | El servidor no está disponible (HTTP 502): volvé a intentarlo en un rato",
        ],
    )
    fun unErrorTraeElMensajeDelBackendONuncaElCuerpoCrudo(caso: String, status: Int, cuerpo: String, esperado: String) = runTest {
        servidor.enqueue(MockResponse().setResponseCode(status).setBody(cuerpo))

        val rechazo = runCatching {
            ApiPuerta(servidor.url("/api").toString(), "p", "c", StandardTestDispatcher(testScheduler)).registrarAcceso("K7M2P9XQ")
        }.exceptionOrNull() as Rechazo
        assertEquals(esperado, rechazo.message)
        assertEquals(status, rechazo.status)
    }

    @Test
    fun noSigueRedireccionesConLaClave() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://otro.example/api/acceso"))

        val rechazo = runCatching {
            ApiPuerta(servidor.url("/api").toString(), "p", "c", StandardTestDispatcher(testScheduler)).registrarAcceso("K7M2P9XQ")
        }.exceptionOrNull() as Rechazo

        assertEquals(302, rechazo.status)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun sinServidorDiceQueNoPudoConectar() = runTest {
        val url = servidor.url("/api").toString()
        servidor.shutdown()

        val rechazo = runCatching {
            ApiPuerta(url, "p", "c", StandardTestDispatcher(testScheduler)).registrarAcceso("K7M2P9XQ")
        }.exceptionOrNull() as Rechazo

        assertEquals("No se pudo conectar con el servidor: revisá la dirección y la red", rechazo.message)
    }
}
