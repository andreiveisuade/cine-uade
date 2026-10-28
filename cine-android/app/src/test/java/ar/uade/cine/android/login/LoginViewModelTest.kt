package ar.uade.cine.android.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val despachador = StandardTestDispatcher()
    private lateinit var servidor: MockWebServer

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

    private fun viewModel() = LoginViewModel(despachador).apply {
        cambiarServidor(servidor.url("/api").toString())
        cambiarUsuario("puerta")
        cambiarClave("clave")
    }

    @Test
    fun conCredencialesBuenasAbreLaSesion() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody("""{"id":3,"nombre":"Puerta","email":"p@x","rol":"ACOMODADOR","empleado":true}"""))
        val vm = viewModel()

        vm.ingresar()
        runCurrent()

        assertEquals("Puerta", vm.estado.value.sesion?.usuario?.nombre)
        assertEquals(false, vm.estado.value.cargando)
    }

    @Test
    fun conUn401MuestraElErrorTalCual() = runTest(despachador) {
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Email o contraseña incorrectos"}"""))
        val vm = viewModel()

        vm.ingresar()
        runCurrent()

        assertEquals("Email o contraseña incorrectos", vm.estado.value.error)
        assertNull(vm.estado.value.sesion)
    }

    @Test
    fun unClienteNoEntra() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody("""{"id":9,"nombre":"Ana","email":"a@x","rol":"CLIENTE","empleado":false}"""))
        val vm = viewModel()

        vm.ingresar()
        runCurrent()

        assertEquals(LoginViewModel.SOLO_EMPLEADOS, vm.estado.value.error)
        assertNull(vm.estado.value.sesion)
    }

    @Test
    fun conUnServidorHttpAInternetNoMandaLaClave() = runTest(despachador) {
        val vm = viewModel()
        vm.cambiarServidor("http://cine-uade.onrender.com/api")

        vm.ingresar()
        runCurrent()

        assertEquals("Fuera de la red local el servidor tiene que ser https", vm.estado.value.error)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun salirOlvidaLaClave() = runTest(despachador) {
        servidor.enqueue(MockResponse().setBody("""{"id":3,"nombre":"Puerta","email":"p@x","rol":"ACOMODADOR","empleado":true}"""))
        val vm = viewModel()
        vm.ingresar()
        runCurrent()

        vm.salir()

        assertEquals("", vm.estado.value.clave)
        assertNull(vm.estado.value.sesion)
    }
}
