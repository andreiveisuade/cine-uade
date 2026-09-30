package ar.uade.cine.service.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.repository.candy.CompraCandyRepository;

// El Observer del ticket de candy: se escribe después del commit, y solo si hubo commit.
class ComprobantesDeCandyTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorCandy candy;
    @Autowired
    private GestorProductos carta;
    @Autowired
    private CompraCandyRepository compras;
    @Autowired
    private PlatformTransactionManager transacciones;

    private int pochoclos;

    @BeforeEach
    void prepararCarta() {
        pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000)).getId();
    }

    @Test
    void elTicketSeEscribeCuandoLaCompraConfirma() {
        CompraCandy compra = candy.vender(null, Map.of(pochoclos, 1), MedioPago.EFECTIVO, "");

        assertTrue(Files.exists(TICKETS.resolve("candy-" + compra.getId() + ".txt")));
    }

    @Test
    void siLaCompraSeDeshaceDespuesDeAvisarNoHayTicket() {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            candy.vender(null, Map.of(pochoclos, 1), MedioPago.EFECTIVO, "");
            estado.setRollbackOnly();
        });

        assertEquals(0, compras.count());
        assertFalse(Files.exists(TICKETS.resolve("candy-1.txt")));
    }
}
