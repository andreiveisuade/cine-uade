package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
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
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.service.informes.GestorInformes;
import ar.uade.cine.service.informes.InformeFuncion;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorReservas;

class AplicacionTest extends PruebaDeIntegracion {

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private ConsultasReservas consultas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorCaja caja;
    @Autowired
    private GestorProductos productos;
    @Autowired
    private GestorCandy candy;
    @Autowired
    private GestorInformes informes;

    @Test
    void elCircuitoDeCompraAtraviesaLosGestoresYaConectados() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(5000.0), pago.getMonto());
        assertEquals(EstadoReserva.PAGADA,
                consultas.buscar(reserva.getId()).orElseThrow().getEstado());
        assertEquals(Dinero.de(5000.0), caja.arqueoDe(pago.getFecha().toLocalDate()).total());
    }

    @Test
    void elCandySeEnganchaConLaReservaRecienHecha() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));

        Producto pochoclos = productos
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        CompraCandy compra = candy.venderParaReserva(reserva.getId(),
                Map.of(pochoclos.getId(), 2), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(6000.0), compra.getTotal());
        assertEquals(cliente.getId(), compra.getClienteId());
        assertTrue(candy.listarComprasDe(cliente.getId()).size() == 1);
    }

    @Test
    void elInformeDeLaFuncionVeLoQueCobraronLosOtrosGestores() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        Reserva reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        Producto pochoclos = productos
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        candy.venderParaReserva(reserva.getId(), Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(funcion.getId());

        assertEquals(1, informe.bordero().espectadores());
        assertEquals(Dinero.de(5000.0), informe.bordero().recaudacionNeta());
        assertEquals(Dinero.de(3000.0), informe.candy());
        assertEquals(Dinero.de(8000.0), informe.total());
    }
}
