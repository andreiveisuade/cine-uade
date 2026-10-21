package ar.uade.cine.infrastructure.comprobantes.txt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

// Cada comprobante renglón por renglón: lo que imprime una persona no cambia aunque cambie cómo se arma.
class ComprobantesTxtTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private GestorCandy candy;
    @Autowired
    private GestorProductos carta;

    @Test
    void elTicketYElReciboDeUnaVenta() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        clientes.identificar("Andrei", "andrei@uade.edu.ar");
        // En orden: las entradas salen como las recorre el mapa, y Map.of no garantiza ninguno.
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.GENERAL);
        butacas.put("A2", TipoTarifa.JUBILADO);
        Reserva reserva = reservas.reservar(1, 1, butacas, null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals("""
                ============================================
                                 CINE UADE
                                 TICKET #1
                ============================================
                 Película     : Matrix
                 Sala         : Sala 1 (2D)
                 Función      : 20/08/2026 20:00
                 Formato      : 2D subtitulada
                 Cliente      : Andrei
                 Emitido      : 14/08/2026 10:00
                ============================================
                 Butaca A1    : $ 5000.00
                 Butaca A2    : $ 2500.00 Jubilado
                ============================================
                 Entradas     : 2
                 Total        : $ 7500.00
                 Estado       : Reservada
                ============================================
                           Presentar en boletería
                ============================================
                """, texto("ticket-" + reserva.getId() + ".txt"));
        assertEquals("""
                ============================================
                                 CINE UADE
                             RECIBO DE CAJA #1
                ============================================
                 Reserva      : #1 - %s
                 Entradas     : 2
                 Cobrado      : 14/08/2026 10:00
                ============================================
                 Subtotal     : $ 7500.00
                 Total        : $ 7500.00
                 Pago         : Efectivo
                ============================================
                      Comprobante de pago en efectivo
                    Conservar hasta el ingreso a la sala
                ============================================
                """.formatted(reserva.getCodigo()), texto("recibo-" + pago.getId() + ".txt"));
    }

    @Test
    void elTicketDeCandy() {
        int pochoclos = carta.agregar("Pochoclos grandes con manteca", TipoProducto.POCHOCLOS, Dinero.de(3000)).getId();
        CompraCandy compra = candy.vender(null, Map.of(pochoclos, 2), MedioPago.EFECTIVO, "");

        assertEquals("""
                ============================================
                             CINE UADE - CANDY
                                 COMPRA #1
                ============================================
                 Cliente      : Consumidor final
                 Fecha        : 14/08/2026 10:00
                ============================================
                 2 x Pochoclos grandes con… $  6000.00
                ============================================
                 Total        : $ 6000.00
                 Pago         : Efectivo
                ============================================
                            Retirar en el candy
                ============================================
                """, texto("candy-" + compra.getId() + ".txt"));
    }

    private static String texto(String archivo) {
        try {
            return Files.readString(TICKETS.resolve(archivo));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
