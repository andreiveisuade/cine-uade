package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.ItemCompra;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TicketCandyTest {

    @Test
    void escribeLaVentaConSusRenglonesAlineados() {
        CompraCandy compra = new CompraCandy(8, 25, "2026-08-13T19:40:00", "DEBITO", "AUTH-40219",
                List.of(new ItemCompra("Pochoclos grandes", 2, 9000), new ItemCompra("Combo pareja", 1, 12000)),
                21000, 1500);

        String esperado = """
                ========================================
                  CINE UADE · CANDY
                  13/08/2026 19:40
                ========================================
                 2x Pochoclos grandes         $ 9000.00
                 1x Combo pareja             $ 12000.00
                ========================================
                 TOTAL                       $ 21000.00
                 Ahorro por combos            $ 1500.00
                 Medio                           Débito
                 Autorizacion                AUTH-40219
                 Reserva                            #25
                ========================================""";
        assertEquals(esperado, TicketCandy.escribir(compra));
    }

    @Test
    void enEfectivoDeMostradorNoHayAutorizacionNiReservaNiAhorro() {
        CompraCandy compra = new CompraCandy(9, null, "2026-08-13T19:45:00", "EFECTIVO", "",
                List.of(new ItemCompra("Agua", 1, 1500)), 1500, 0);

        String ticket = TicketCandy.escribir(compra);

        assertFalse(ticket.contains("Autorizacion"));
        assertFalse(ticket.contains("Reserva"));
        assertFalse(ticket.contains("Ahorro"));
    }

    @Test
    void unNombreLargoSeCortaParaNoPisarElImporte() {
        CompraCandy compra = new CompraCandy(10, null, "2026-08-13T20:00:00", "EFECTIVO", null,
                List.of(new ItemCompra("Pochoclos extra grandes con manteca", 1, 6000)), 6000, 0);

        String renglon = TicketCandy.escribir(compra).lines().toList().get(4);

        assertEquals(" 1x Pochoclos extra grandes   $ 6000.00", renglon);
    }
}
