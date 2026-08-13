package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * El armado de la aplicación, probado sin MySQL de por medio: el mismo constructor que
 * usa {@link Aplicacion#enMySQL()}, con las implementaciones en memoria.
 *
 * <p>Esto es lo que faltaba cuando cada arranque armaba su propia aplicación. Un gestor
 * que quedaba afuera —o dos gestores conectados a DAOs distintos, que es peor porque no
 * falla, simplemente no se ven entre sí— no rompía ningún test: se descubría usando la
 * app. Acá el circuito completo los atraviesa a todos.
 */
class AplicacionTest {

    @TempDir
    Path tempDir;

    private Aplicacion aplicacion;

    @BeforeEach
    void armarLaAplicacionEnMemoria() {
        aplicacion = new Aplicacion(
                new PeliculaDAOMemoria(), new SalaDAOMemoria(), new AsientoDAOMemoria(),
                new FuncionDAOMemoria(), new ClienteDAOMemoria(), new EmpleadoDAOMemoria(),
                new ReservaDAOMemoria(), new PagoDAOMemoria(),
                new PromocionDAOMemoria(), new ProgramacionDAOMemoria(), new ProductoDAOMemoria(),
                new CompraCandyDAOMemoria(),
                new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")));
    }

    @Test
    void quedanArmadosLosOnceGestores() {
        assertNotNull(aplicacion.getCartelera());
        assertNotNull(aplicacion.getSalas());
        assertNotNull(aplicacion.getFunciones());
        assertNotNull(aplicacion.getProgramaciones());
        assertNotNull(aplicacion.getClientes());
        assertNotNull(aplicacion.getEmpleados());
        assertNotNull(aplicacion.getPromociones());
        assertNotNull(aplicacion.getPagos());
        assertNotNull(aplicacion.getReservas());
        // El que faltaba en la puerta HTTP cuando el armado estaba duplicado.
        assertNotNull(aplicacion.getCandy());
        assertNotNull(aplicacion.getProductos());
        assertNotNull(aplicacion.getOcupacion());
    }

    /**
     * Comprar una entrada de punta a punta. Cada paso lo atiende un gestor distinto, así
     * que si dos quedaran conectados a DAOs distintos, alguno no encontraría lo que el
     * anterior acaba de guardar.
     */
    @Test
    void elCircuitoDeCompraAtraviesaLosGestoresYaConectados() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = aplicacion.getFunciones().programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        Cliente cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = aplicacion.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));
        Pago pago = aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(5000.0, pago.getMonto(), 0.001);
        assertEquals(EstadoReserva.PAGADA,
                aplicacion.getReservas().buscar(reserva.getId()).orElseThrow().getEstado());
        // El arqueo lo arma GestorPagos y tiene que ver el cobro que acaba de entrar.
        assertEquals(5000.0, aplicacion.getPagos().arqueoDe(pago.getFecha().toLocalDate()).total(), 0.001);
    }

    /**
     * El candy engancha con la reserva: es el «¿desea agregar pochoclos?» de después de
     * comprar la entrada. Que funcione prueba que GestorCandy y GestorReservas comparten
     * el mismo ReservaDAO.
     */
    @Test
    void elCandySeEnganchaConLaReservaRecienHecha() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = aplicacion.getFunciones().programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        Cliente cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = aplicacion.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));

        Producto pochoclos = aplicacion.getProductos()
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, 3000);
        CompraCandy compra = aplicacion.getCandy().venderParaReserva(reserva.getId(),
                Map.of(pochoclos.getId(), 2), MedioPago.EFECTIVO, "");

        assertEquals(6000.0, compra.getTotal(), 0.001);
        // El cliente sale de la reserva, no se vuelve a pedir.
        assertEquals(cliente.getId(), compra.getClienteId());
        assertTrue(aplicacion.getCandy().listarComprasDe(cliente.getId()).size() == 1);
    }
}
