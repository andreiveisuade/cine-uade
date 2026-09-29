package ar.uade.cine.controller.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

class CompraCandyControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos carta;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;

    @Test
    void unaVentaConUnaCantidadEnNullEs400YNo500() {
        int pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)).getId();

        Respuesta respuesta = post("/api/candy/compras",
                "{\"cantidades\":{\"" + pochoclos + "\":null},\"medio\":\"EFECTIVO\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la cantidad de Pochoclos grandes", respuesta.error());
    }

    // Antes el controller descartaba el clienteId cuando venía la reserva, y la compra quedaba a nombre
    // del dueño de la reserva sin avisar.
    @Test
    void unaVentaParaLaReservaDeOtroClienteEs400() {
        int pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)).getId();
        int duenio = clientes.registrar("Andrei", "andrei@uade.edu.ar").getId();
        int otro = clientes.registrar("Otra", "otra@uade.edu.ar").getId();
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5));
        funciones.programar(1, 1, reloj.ahora().plusDays(1), Version.SUBTITULADA, Proyeccion.DOS_D,
                Dinero.de(5000));
        int reserva = reservas.reservar(1, duenio, Map.of("A1", TipoTarifa.GENERAL), null).getId();
        pagos.cobrar(reserva, MedioPago.EFECTIVO, "");

        Respuesta respuesta = post("/api/candy/compras", "{\"clienteId\":" + otro + ",\"reservaId\":" + reserva
                + ",\"cantidades\":{\"" + pochoclos + "\":1},\"medio\":\"EFECTIVO\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("La reserva " + reserva + " es de otro cliente: revisá la reserva o el cliente",
                respuesta.error());
    }
}
