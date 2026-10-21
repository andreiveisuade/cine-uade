package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contrato real: las Api de Swing contra el backend levantado, que ServidorFalso no ve cambiar. `mvn -Phumo test`.
// Lo que crea lo borra; la cuenta de cliente es una sola y fija, porque el backend no da de baja clientes.
@Tag("humo")
class HumoContraBackendTest {

    private static final String CLAVE = "cine2026";
    private static final String CLIENTE = "humo-swing@cine.uade.ar";

    private final String url = ClienteHttp.urlConfigurada();

    @Test
    void elEncargadoRecorreSalasCatalogoFuncionesYReservas() {
        Apis apis = Apis.sobre(new ClienteHttp(url));
        assertTrue(apis.sesion().login("encargado@cine.uade.ar", CLAVE).esAdministrador());

        List<Sala> salas = apis.salas().obtenerSalas();
        assertFalse(salas.isEmpty(), "sin salas: falta correr seed/datos-de-ejemplo.sh");
        assertFalse(apis.catalogos().obtenerTiposSala().isEmpty());
        assertFalse(apis.catalogos().obtenerGeneros().isEmpty());
        apis.ventas().obtenerReservas(Map.of());

        // El seed no trae películas: se crea una propia para colgarle la función.
        Pelicula pelicula = apis.cartelera().crearPelicula(new PedidoPelicula("Humo Swing " + System.nanoTime(),
                100, List.of("DRAMA"), "ATP", null, null, null, null, null, null, null));
        try {
            Sala sala = salas.stream().filter(s -> "DOS_D".equals(s.tipo())).findFirst().orElseThrow();
            Funcion funcion = apis.funciones().programarFuncion(new PedidoFuncion(pelicula.id(), sala.id(),
                    LocalDate.now().plusDays(200) + "T20:00:00", "SUBTITULADA", "DOS_D", 5000.0));
            try {
                assertEquals(pelicula.id(), apis.funciones().obtenerFuncion(funcion.id()).pelicula().id());
            } finally {
                apis.funciones().eliminarFuncion(funcion.id());
            }
        } finally {
            apis.cartelera().eliminarPelicula(pelicula.id());
        }
    }

    @Test
    void unClienteNoEntraAlPanel() throws Exception {
        abrirCuentaDeCliente();
        ClienteHttp http = new ClienteHttp(url);
        ErrorApi error = assertThrows(ErrorApi.class, () -> new ApiSesion(http).login(CLIENTE, CLAVE));
        assertEquals(401, error.estado());
        assertTrue(error.getMessage().contains("los clientes compran en la web"));
    }

    @Test
    void elAcomodadorEntraSinSerAdministrador() {
        assertEquals("ACOMODADOR", new ApiSesion(new ClienteHttp(url)).login("puerta@cine.uade.ar", CLAVE).rol());
    }

    // Swing no da de alta clientes, así que va directo; el 409 de las corridas siguientes dice que ya existe.
    private void abrirCuentaDeCliente() throws Exception {
        String cuerpo = "{\"nombre\":\"Humo Swing\",\"email\":\"" + CLIENTE + "\",\"password\":\"" + CLAVE + "\"}";
        HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url.replaceAll("/+$", "") + "/api/clientes"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(respuesta.statusCode() == 201 || respuesta.statusCode() == 409, respuesta.body());
    }
}
