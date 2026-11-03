package ar.uade.cine.android.resultado

import ar.uade.cine.android.api.CompraCandy
import ar.uade.cine.android.api.ItemCompra
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FormatoTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "hora de la funcion, hora, 2026-10-22T20:30:00, 20:30",
        "tarifa simple, tarifa, JUBILADO, Jubilado",
        "tarifa compuesta, tarifa, MAS_16, Mas 16",
    )
    fun traduce(caso: String, que: String, entrada: String, esperado: String) {
        assertEquals(esperado, if (que == "hora") Formato.hora(entrada) else Formato.tarifa(entrada))
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource("una, 1, 1 persona", "varias, 3, 3 personas")
    fun personas(caso: String, cantidad: Int, esperado: String) {
        assertEquals(esperado, Formato.personas(cantidad))
    }

    @Test
    fun losItemsDeVariasComprasVanUnoPorRenglon() {
        val compras = listOf(
            CompraCandy(items = listOf(ItemCompra("Agua", 2))),
            CompraCandy(items = listOf(ItemCompra("Combo clásico", 1))),
        )
        assertEquals(listOf("2 × Agua", "1 × Combo clásico"), Formato.items(compras))
    }
}
