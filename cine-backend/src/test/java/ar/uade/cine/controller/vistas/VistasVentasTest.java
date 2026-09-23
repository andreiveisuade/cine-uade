package ar.uade.cine.controller.vistas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorAcceso;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.model.dinero.Dinero;

/**
 * La reserva viaja como ticket completo. Lo crítico es lo que usa la puerta: el código del QR
 * e ingresadaEn, en null mientras nadie entró.
 */
class VistasVentasTest extends PruebaDeIntegracion {

    @Autowired
    private CalculadoraPrecio calculadoraPrecio;

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorClientes clientes;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorPagos pagos;

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorAcceso acceso;
    @Autowired
    private ConsultasReservas consultas;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private Ocupacion ocupacion;

    @Autowired
    private VistasVentas vistas;

    private Cliente cliente;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
    }

    @Test
    void laReservaTraeTodoLoQueImprimeElTicket() {
        ReservaVistaDTO vista = vistas.reserva(reservar("A1", "A2"));

        assertEquals("Matrix", vista.pelicula().titulo());
        assertEquals("Sala 1", vista.sala().nombre());
        assertEquals("2026-08-20T20:00:00", vista.funcion().inicio());
        assertEquals("Andrei", vista.cliente().nombre());
        assertEquals(2, vista.cantidadEntradas());
        assertEquals(10000.0, vista.total());
        assertEquals("RESERVADA", vista.estado());
    }

    /** Se identifica por email porque compra sin registrarse. */
    @Test
    void elClienteViajaSinDatosDeMas() {
        ReservaVistaDTO vista = vistas.reserva(reservar("A1"));

        assertEquals(cliente.getId(), vista.cliente().id());
        assertEquals("andrei@uade.edu.ar", vista.cliente().email());
    }

    /** Para que el acomodador pida el carnet a quien pagó tarifa reducida. */
    @Test
    void cadaEntradaDiceConQueTarifaSeVendio() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.GENERAL);
        butacas.put("A2", TipoTarifa.JUBILADO);

        ReservaVistaDTO vista = vistas.reserva(reservas
                .reservar(1, cliente.getId(), butacas));

        assertEquals("GENERAL", entrada(vista, "A1").tarifa());
        assertEquals("JUBILADO", entrada(vista, "A2").tarifa());
        assertTrue(entrada(vista, "A2").precio() < entrada(vista, "A1").precio());
    }

    /** El código no es el id: no se puede adivinar. */
    @Test
    void laReservaViajaConElCodigoDelQr() {
        Reserva reserva = reservar("A1");

        ReservaVistaDTO vista = vistas.reserva(reserva);

        assertEquals(reserva.getCodigo(), vista.codigo());
        assertTrue(!vista.codigo().equals(String.valueOf(vista.id())));
    }

    @Test
    void ingresadaEnEstaEnNullHastaQueAlguienEntra() {
        Reserva reserva = reservar("A1");
        assertNull(vistas.reserva(reserva).ingresadaEn());

        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        Reserva ingresada = acceso.registrarIngreso(reserva.getCodigo());

        assertNotNull(vistas.reserva(ingresada).ingresadaEn());
        assertEquals(19, vistas.reserva(ingresada).ingresadaEn().length(),
                "tiene que usar el mismo formato que el resto de las fechas");
    }

    @Test
    void sinCobrarLaReservaViajaSinPago() {
        assertNull(vistas.reserva(reservar("A1")).pago());
    }

    @Test
    void cobradaLaReservaTraeSuPagoYCambiaDeEstado() {
        Reserva reserva = reservar("A1");
        pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "AUTH-123");

        ReservaVistaDTO vista = vistas.reserva(consultas
                .buscar(reserva.getId()).orElseThrow());

        assertEquals("PAGADA", vista.estado());
        assertEquals("CREDITO", vista.pago().medio());
        assertEquals("AUTH-123", vista.pago().codigoAutorizacion());
        assertEquals(5000.0, vista.pago().monto());
    }

    @Test
    void elPagoEmbebidoNoRepiteLoQueYaTraeLaReserva() {
        Reserva reserva = reservar("A1");
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        PagoVistaDTO vista = vistas.pago(pago);

        assertNull(vista.pelicula());
        assertNull(vista.cliente());
        assertNull(vista.entradas());
    }

    /** En el arqueo no hay reserva alrededor para consultar película y cliente. */
    @Test
    void elPagoDelArqueoSiTraeQueSeVendioYAQuien() {
        Reserva reserva = reservar("A1", "A2");
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        PagoVistaDTO vista = vistas.pagoDeArqueo(pago);

        assertEquals("Matrix", vista.pelicula().titulo());
        assertEquals("Andrei", vista.cliente().nombre());
        assertEquals(2, vista.entradas());
        assertEquals(10000.0, vista.monto());
    }

    private Reserva reservar(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return reservas.reservar(1, cliente.getId(), butacas);
    }

    private static EntradaVistaDTO entrada(ReservaVistaDTO vista, String codigo) {
        return vista.entradas().stream()
                .filter(e -> e.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La reserva no tiene la butaca " + codigo));
    }
}
