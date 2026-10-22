package ar.uade.cine.service.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.ResumenDeCorrida;

class CorridaTest {

    @Test
    void sinCandidatasElResumenQuedaEnCeroYSinDetalle() {
        ResumenDeCorrida resumen = new Corrida().resumen();

        assertEquals(new ResumenDeCorrida(0, 0, 0, null), resumen);
        assertNull(resumen.detalle());
    }

    @Test
    void cuentaCadaResultadoYArmaUnRenglonPorPelicula() {
        Corrida corrida = new Corrida();

        corrida.nueva(7, "Duna");
        corrida.salteada("Cars");
        corrida.fallida("Corto de festival", "La duración tiene que ser positiva");

        assertEquals(new ResumenDeCorrida(1, 1, 1,
                "+ [7] Duna\n= Cars\n✗ Corto de festival\tLa duración tiene que ser positiva"), corrida.resumen());
    }
}
