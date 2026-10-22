package ar.uade.cine.android.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ServidorTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "render por https, https://cine-uade.onrender.com/api/, https://cine-uade.onrender.com/api",
        "red de casa, http://192.168.0.10:8080/api, http://192.168.0.10:8080/api",
        "red 10, http://10.1.2.3/api, http://10.1.2.3/api",
        "red 172.16, http://172.20.0.5/api, http://172.20.0.5/api",
        "emulador, http://10.0.2.2:8080/api, http://10.0.2.2:8080/api",
        "localhost, http://localhost:8080/api, http://localhost:8080/api",
    )
    fun aceptaHttpsOHttpEnLaRedPropia(caso: String, texto: String, esperado: String) {
        assertEquals(esperado, Servidor.validar(texto).toString().trimEnd('/'))
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "http a internet, http://cine-uade.onrender.com/api, Fuera de la red local el servidor tiene que ser https",
        "ip publica por http, http://8.8.8.8/api, Fuera de la red local el servidor tiene que ser https",
        "parece privada pero no, http://192.168.evil.com/api, Fuera de la red local el servidor tiene que ser https",
        "172 fuera de rango, http://172.32.0.1/api, Fuera de la red local el servidor tiene que ser https",
        "sin esquema, cine-uade.onrender.com, La dirección del servidor no es válida",
        "otro esquema, ftp://x/api, La dirección del servidor no es válida",
    )
    fun rechazaLoQuePondriaLaClaveEnRiesgo(caso: String, texto: String, mensaje: String) {
        assertEquals(mensaje, assertThrows<Rechazo> { Servidor.validar(texto) }.message)
    }
}
