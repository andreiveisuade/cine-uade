package ar.uade.cine.controller.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
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

    private int pochoclos;
    private int gaseosa;

    private void cargarCarta() {
        pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA)
                .getId();
        gaseosa = carta.agregar("Gaseosa 500ml", TipoProducto.BEBIDA, Dinero.de(2500), ImagenUrl.NINGUNA).getId();
    }

    private int reservaPagadaDe(int clienteId) {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5));
        funciones.programar(1, 1, reloj.ahora().plusDays(1),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        int reserva = reservas.reservar(1, clienteId, Map.of("A1", TipoTarifa.GENERAL), null).getId();
        pagos.cobrar(reserva, MedioPago.EFECTIVO, "");
        return reserva;
    }

    @Test
    void unaVentaDeMostradorEs201ConSusItemsYSuTotal() {
        cargarCarta();

        Respuesta respuesta = post("/api/candy/compras", "{\"cantidades\":{\"" + pochoclos + "\":2,\"" + gaseosa
                + "\":1},\"medio\":\"DEBITO\",\"codigoAutorizacion\":\"AUT-1\"}");

        assertEquals(201, respuesta.estado());
        assertEquals("{\"id\":1,\"estado\":\"COBRADA\",\"fecha\":\"2026-08-14T10:00:00\","
                + "\"cobradaEn\":\"2026-08-14T10:00:00\",\"medio\":\"DEBITO\","
                + "\"codigoAutorizacion\":\"AUT-1\","
                + "\"items\":[{\"productoId\":" + pochoclos + ",\"nombre\":\"Pochoclos grandes\",\"cantidad\":2,"
                + "\"precioUnitario\":4000.0,\"subtotal\":8000.0},{\"productoId\":" + gaseosa
                + ",\"nombre\":\"Gaseosa 500ml\",\"cantidad\":1,\"precioUnitario\":2500.0,\"subtotal\":2500.0}],"
                + "\"total\":10500.0,\"ahorro\":0.0}", respuesta.cuerpo());
    }

    // Sin clienteId la compra queda a nombre del dueño de la reserva.
    @Test
    void unaVentaParaUnaReservaSinClienteHeredaElDeLaReserva() {
        cargarCarta();
        int duenio = clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar").getId();
        int reserva = reservaPagadaDe(duenio);

        Respuesta respuesta = post("/api/candy/compras", "{\"reservaId\":" + reserva
                + ",\"cantidades\":{\"" + pochoclos + "\":1},\"medio\":\"EFECTIVO\"}");

        assertEquals(201, respuesta.estado());
        assertEquals(duenio, respuesta.json().get("clienteId").asInt());
        assertEquals(reserva, respuesta.json().get("reservaId").asInt());
    }

    @Test
    void lasComprasSeListanPorDiaOPorCliente() {
        cargarCarta();
        int andrei = clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar").getId();
        post("/api/candy/compras", "{\"cantidades\":{\"" + pochoclos + "\":1},\"medio\":\"EFECTIVO\"}");
        post("/api/candy/compras", "{\"clienteId\":" + andrei + ",\"cantidades\":{\"" + gaseosa
                + "\":1},\"medio\":\"EFECTIVO\"}");

        assertEquals(2, get("/api/candy/compras?fecha=" + reloj.hoy()).json().size());
        assertEquals(0, get("/api/candy/compras?fecha=" + reloj.hoy().minusDays(1)).json().size());
        Respuesta delCliente = get("/api/candy/compras?clienteId=" + andrei);
        assertEquals(1, delCliente.json().size());
        assertEquals(andrei, delCliente.json().get(0).get("clienteId").asInt());
    }

    @Test
    void listarSinFechaNiClienteEs400YNo500() {
        Respuesta respuesta = get("/api/candy/compras");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la fecha", respuesta.error());
    }

    @Test
    void unaVentaConUnaCantidadEnNullEs400YNo500() {
        cargarCarta();

        Respuesta respuesta = post("/api/candy/compras",
                "{\"cantidades\":{\"" + pochoclos + "\":null},\"medio\":\"EFECTIVO\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la cantidad de Pochoclos grandes", respuesta.error());
    }

    // El medio se valida antes de buscar los productos: un código en efectivo es error de carga aunque falte uno.
    @Test
    void elEfectivoConCodigoSeRechazaAntesDeBuscarLosProductos() {
        Respuesta respuesta = post("/api/candy/compras",
                "{\"cantidades\":{\"99\":1},\"medio\":\"EFECTIVO\",\"codigoAutorizacion\":\"AUT-1\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El pago en efectivo no lleva código de autorización", respuesta.error());
    }

    // El clienteId se respeta aunque venga la reserva: no se compra a nombre del dueño sin avisar.
    @Test
    void unaVentaParaLaReservaDeOtroClienteEs400() {
        cargarCarta();
        int duenio = clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar").getId();
        int otro = clientes.ingresarConGoogle("Otra", "otra@uade.edu.ar").getId();
        int reserva = reservaPagadaDe(duenio);

        Respuesta respuesta = post("/api/candy/compras", "{\"clienteId\":" + otro + ",\"reservaId\":" + reserva
                + ",\"cantidades\":{\"" + pochoclos + "\":1},\"medio\":\"EFECTIVO\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("La reserva " + reserva + " es de otro cliente: revisá la reserva o el cliente",
                respuesta.error());
    }
}
