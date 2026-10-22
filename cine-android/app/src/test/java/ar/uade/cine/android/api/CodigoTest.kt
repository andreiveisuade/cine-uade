package ar.uade.cine.android.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CodigoTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "ya normalizado, K7M2P9XQ, K7M2P9XQ",
        "minusculas, k7m2p9xq, K7M2P9XQ",
        "espacios alrededor, '  K7M2P9XQ ', K7M2P9XQ",
        "espacios en el medio, 'K7M2 P9XQ', K7M2P9XQ",
    )
    fun normaliza(caso: String, texto: String, esperado: String) {
        assertEquals(esperado, Codigo.normalizar(texto))
    }
}
