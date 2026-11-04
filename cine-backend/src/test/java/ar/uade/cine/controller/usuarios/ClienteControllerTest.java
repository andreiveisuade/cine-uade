package ar.uade.cine.controller.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorReservas;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpMethod;

class ClienteControllerTest extends PruebaDeApi {

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
    private GestorCandy candy;
    @Autowired
    private GestorProductos carta;

    // Un filtro sobre la colección: siempre una lista, con el cliente o vacía; nunca el literal null.
    @Test
    void laBusquedaPorEmailEsUnaListaQueIgnoraLosEspacios() {
        clientes.ingresarConGoogle("Ana", "ana@mail.com");

        JsonNode encontrados = get("/api/clientes?email=%20ana@mail.com%20").json();
        assertEquals(1, encontrados.size());
        assertEquals("ana@mail.com", encontrados.get(0).get("email").asText());
        assertEquals("[]", get("/api/clientes?email=nadie@mail.com").cuerpo());
        assertEquals("[]", get("/api/clientes?email=%20%20").cuerpo());
    }

    @Test
    void elListadoTraeATodosPorNombreConElEmailSinEnmascarar() {
        clientes.ingresarConGoogle("Zoe", "zoe@mail.com");
        clientes.ingresarConGoogle("Ana", "ana@mail.com");

        JsonNode todos = get("/api/clientes").json();

        assertEquals(2, todos.size());
        assertEquals("Ana", todos.get(0).get("nombre").asText());
        assertEquals("ana@mail.com", todos.get(0).get("email").asText());
    }

    // locate y no like: un % tipeado se busca como texto, no como comodín.
    @Test
    void elFiltroBuscaEnNombreOEmailSinImportarMayusculas() {
        clientes.ingresarConGoogle("Ana Prueba", "ana@mail.com");
        clientes.ingresarConGoogle("Zoe", "zoe@otro.com");

        assertEquals("Ana Prueba", get("/api/clientes?q=PRUE").json().get(0).get("nombre").asText());
        assertEquals("Zoe", get("/api/clientes?q=OTRO.com").json().get(0).get("nombre").asText());
        assertEquals(2, get("/api/clientes?q=%20%20").json().size());
        assertEquals("[]", get("/api/clientes?q=%25").cuerpo());
    }

    @Test
    void lasTransaccionesTraenSusReservasConCualquierEstadoYSuCandySuelto() {
        Cliente ana = clientes.ingresarConGoogle("Ana", "ana@mail.com");
        Cliente otra = clientes.ingresarConGoogle("Otra", "otra@mail.com");
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        int sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5)).getId();
        int funcion = funciones.programar(1, sala, LocalDateTime.of(2026, 9, 1, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))).getId();
        int pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA).getId();
        Reserva cancelada = reservas.reservar(funcion, ana.getId(), Map.of("A1", TipoTarifa.GENERAL), null);
        reservas.cancelar(cancelada.getId());
        Reserva conCandy = reservas.reservar(funcion, ana.getId(), Map.of("A2", TipoTarifa.GENERAL), null);
        candy.pedirParaReserva(conCandy.getId(), Map.of(pochoclos, 1));
        reservas.reservar(funcion, otra.getId(), Map.of("A3", TipoTarifa.GENERAL), null);
        candy.vender(ana.getId(), Map.of(pochoclos, 2), MedioPago.EFECTIVO, "");

        JsonNode transacciones = get("/api/clientes/" + ana.getId() + "/transacciones").json();

        JsonNode suyas = transacciones.get("reservas");
        assertEquals(2, suyas.size());
        assertEquals(conCandy.getId(), suyas.get(0).get("id").asInt(), "la más nueva primero");
        assertEquals("CANCELADA", suyas.get(1).get("estado").asText());
        assertEquals("ana@mail.com", suyas.get(0).get("cliente").get("email").asText());
        assertEquals(1, suyas.get(0).get("candy").size());
        JsonNode sueltas = transacciones.get("comprasCandy");
        assertEquals(1, sueltas.size(), "el candy de la reserva ya viene en ella");
        assertEquals(8000, sueltas.get(0).get("total").asDouble());
    }

    @Test
    void lasTransaccionesDeUnClienteQueNoExisteSonUn404() {
        Respuesta respuesta = get("/api/clientes/999/transacciones");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe el cliente 999", respuesta.error());
    }

    @Test
    void elListadoYLasTransaccionesSonSoloDelEncargado() {
        int ana = clientes.ingresarConGoogle("Ana", "ana@mail.com").getId();
        String transacciones = "/api/clientes/" + ana + "/transacciones";

        assertEquals(401, pedirComo(HttpMethod.GET, "/api/clientes", null, null, null).estado());
        assertEquals(401, pedirComo(HttpMethod.GET, transacciones, null, null, null).estado());
        assertEquals(403, conSesion(HttpMethod.GET, "/api/clientes", entrarConGoogle("ana@mail.com"), null).estado());
        assertEquals(403, conSesion(HttpMethod.GET, transacciones, entrarConGoogle("ana@mail.com"), null).estado());
    }
}
