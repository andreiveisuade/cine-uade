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

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.service.informes.InformeFuncion;
import ar.uade.cine.repository.memoria.AsientoDAOMemoria;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.repository.memoria.ClienteDAOMemoria;
import ar.uade.cine.repository.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.repository.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.repository.memoria.FuncionDAOMemoria;
import ar.uade.cine.repository.memoria.ImportacionDAOMemoria;
import ar.uade.cine.repository.memoria.PagoDAOMemoria;
import ar.uade.cine.repository.memoria.PeliculaDAOMemoria;
import ar.uade.cine.repository.memoria.ProductoDAOMemoria;
import ar.uade.cine.repository.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.repository.memoria.PromocionDAOMemoria;
import ar.uade.cine.repository.memoria.ReservaDAOMemoria;
import ar.uade.cine.repository.memoria.SalaDAOMemoria;
import ar.uade.cine.model.dinero.Dinero;

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
                new CompraCandyDAOMemoria(), new ImportacionDAOMemoria(),
                new BloqueoButacasMemoria(),
                new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")),
                new GeneradorReciboTxt(tempDir.resolve("tickets")),
                new GeneradorBorderoTxt(tempDir.resolve("informes")),
                new MercadoPagoEmulado(), new CatalogoDePrueba());
    }

    @Test
    void quedanArmadosLosGestores() {
        assertNotNull(aplicacion.getCartelera());
        assertNotNull(aplicacion.getSalas());
        assertNotNull(aplicacion.getFunciones());
        assertNotNull(aplicacion.getProgramaciones());
        assertNotNull(aplicacion.getPlanificadorGrilla());
        assertNotNull(aplicacion.getClientes());
        assertNotNull(aplicacion.getEmpleados());
        assertNotNull(aplicacion.getPromociones());
        assertNotNull(aplicacion.getPagos());
        assertNotNull(aplicacion.getReservas());
        // El que faltaba en la puerta HTTP cuando el armado estaba duplicado.
        assertNotNull(aplicacion.getCandy());
        assertNotNull(aplicacion.getProductos());
        assertNotNull(aplicacion.getOcupacion());
        assertNotNull(aplicacion.getInformes());
        assertNotNull(aplicacion.getRevisionCartelera());
        assertNotNull(aplicacion.getImportaciones());
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
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        Cliente cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = aplicacion.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));
        Pago pago = aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(5000.0), pago.getMonto());
        assertEquals(EstadoReserva.PAGADA,
                aplicacion.getReservas().buscar(reserva.getId()).orElseThrow().getEstado());
        // El arqueo lo arma GestorCaja y tiene que ver el cobro que acaba de entrar.
        assertEquals(Dinero.de(5000.0), aplicacion.getCaja().arqueoDe(pago.getFecha().toLocalDate()).total());
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
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = aplicacion.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));

        Producto pochoclos = aplicacion.getProductos()
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        CompraCandy compra = aplicacion.getCandy().venderParaReserva(reserva.getId(),
                Map.of(pochoclos.getId(), 2), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(6000.0), compra.getTotal());
        // El cliente sale de la reserva, no se vuelve a pedir.
        assertEquals(cliente.getId(), compra.getClienteId());
        assertTrue(aplicacion.getCandy().listarComprasDe(cliente.getId()).size() == 1);
    }

    /**
     * El informe de una función es el gestor que más lejos mira: cruza lo que guardaron
     * reservas, pagos y candy. Si quedara colgado de otros DAOs, no fallaría nada — daría
     * cero, que es la clase de error que este test existe para encontrar.
     */
    @Test
    void elInformeDeLaFuncionVeLoQueCobraronLosOtrosGestores() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = aplicacion.getFunciones().programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = aplicacion.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));
        aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        Producto pochoclos = aplicacion.getProductos()
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        aplicacion.getCandy().venderParaReserva(reserva.getId(), Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        InformeFuncion informe = aplicacion.getInformes().informeDe(funcion.getId());

        assertEquals(1, informe.bordero().espectadores());
        assertEquals(Dinero.de(5000.0), informe.bordero().recaudacionNeta());
        assertEquals(Dinero.de(3000.0), informe.candy());
        assertEquals(Dinero.de(8000.0), informe.total());
    }
}
