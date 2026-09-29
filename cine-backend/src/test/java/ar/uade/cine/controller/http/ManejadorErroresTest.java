package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.rechazos.ButacaOcupada;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.usuarios.ClienteRepository;

class ManejadorErroresTest extends PruebaDeApi {

    @Autowired
    private TestRestTemplate cliente;

    // Rutas que solo existen en este test: provocan lo que el uso normal de la API no provoca a pedido.
    // Clases miembro de un @TestConfiguration: Spring las registra solas, sin @Bean.
    @TestConfiguration
    static class RutasDePrueba {

        static final String RUTA_CHOQUE = "/api/prueba/choque";

        // Una carrera no se repite a pedido: graba dos clientes con el mismo email por el repositorio,
        // salteando al gestor, como dos altas simultáneas que pasaron su existsBy… La base rechaza la
        // segunda con la excepción de verdad.
        @RestController
        static class Choque {

            private final ClienteRepository clientes;

            Choque(ClienteRepository clientes) {
                this.clientes = clientes;
            }

            @PostMapping(RUTA_CHOQUE)
            public void chocar() {
                clientes.save(new Cliente("Ana", "ana@mail.com"));
                clientes.save(new Cliente("Otra Ana", "ana@mail.com"));
            }
        }

        static final String RUTA_RECHAZOS = "/api/prueba/rechazo/";

        // Cada tipo de rechazo, y una IllegalArgumentException suelta como la que tiraría una librería.
        @RestController
        static class Rechazos {

            @PostMapping(RUTA_RECHAZOS + "{tipo}")
            public void rechazar(@PathVariable String tipo) {
                throw switch (tipo) {
                    case "dato" -> new DatoInvalido("Falta el nombre");
                    case "inexistente" -> new RecursoNoEncontrado("No existe la sala 7");
                    case "conflicto" -> new ConflictoDeNegocio("Ya existe una sala con ese nombre");
                    case "butaca" -> new ButacaOcupada("La butaca B4 ya está ocupada");
                    default -> new IllegalArgumentException("Illegal base64 character 2d");
                };
            }
        }

        static final String RUTA_CABECERA = "/api/prueba/cabecera";

        static final String RUTA_ESTADOS = "/api/prueba/estado/";

        // Lo que Spring rechaza sin un handler propio: su detail viene en inglés.
        @RestController
        static class DeSpring {

            @GetMapping(RUTA_CABECERA)
            public void conCabecera(@RequestHeader("X-Prueba") String valor) {
            }

            @PostMapping(RUTA_ESTADOS + "{estado}")
            public void conEstado(@PathVariable int estado) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(estado));
            }
        }
    }

    @Test
    void unaCabeceraQueFaltaEs400EnCastellano() {
        Respuesta respuesta = get(RutasDePrueba.RUTA_CABECERA);

        assertEquals(400, respuesta.estado());
        assertEquals("El pedido no es válido", respuesta.error());
    }

    // El detail de Spring está en inglés: el status se conserva y el texto sale en castellano.
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(textBlock = """
            404, No existe lo que se pidió
            418, El pedido no es válido
            503, 'El servidor no está disponible: volvé a intentarlo en un rato'
            501, Ocurrió un error inesperado en el servidor
            """)
    void loQueSpringRechazaSinHandlerPropioSaleEnCastellanoSegunElStatus(int estado, String mensaje) {
        Respuesta respuesta = post(RutasDePrueba.RUTA_ESTADOS + estado, null);

        assertEquals(estado, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(textBlock = """
            dato,        400, Falta el nombre
            inexistente, 404, No existe la sala 7
            conflicto,   409, Ya existe una sala con ese nombre
            butaca,      409, La butaca B4 ya está ocupada
            """)
    void cadaRechazoSaleConSuStatusYSuTextoIntacto(String tipo, int estado, String mensaje) {
        Respuesta respuesta = post(RutasDePrueba.RUTA_RECHAZOS + tipo, null);

        assertEquals(estado, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    // Lo tiró una librería o un bug: su texto es técnico y no es para el usuario.
    @Test
    void unaIllegalArgumentExceptionQueNoEsRechazoEs500Generico() {
        Respuesta respuesta = post(RutasDePrueba.RUTA_RECHAZOS + "de-libreria", null);

        assertEquals(500, respuesta.estado());
        assertEquals("Ocurrió un error inesperado en el servidor", respuesta.error());
    }

    // Rechazo es sellada: un tipo nuevo sin handler caería en el de IllegalArgumentException, un 500.
    @Test
    void cadaTipoDeRechazoTieneSuHandler() {
        Set<Class<?>> atendidos = Arrays.stream(ManejadorErrores.class.getDeclaredMethods())
                .map(metodo -> metodo.getAnnotation(ExceptionHandler.class))
                .filter(Objects::nonNull)
                .flatMap(handler -> Arrays.stream(handler.value()))
                .collect(Collectors.toSet());

        for (Class<?> rechazo : Rechazo.class.getPermittedSubclasses()) {
            assertTrue(atendidos.contains(rechazo), "ManejadorErrores no atiende " + rechazo.getSimpleName());
        }
    }

    @Test
    void credencialesEquivocadasSon401() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "nadie@cine.com", "otracosa");

        assertEquals(401, respuesta.estado());
        assertEquals("Email o contraseña incorrectos", respuesta.error());
    }

    @Test
    void unMetodoQueLaRutaNoAceptaEs405() {
        Respuesta respuesta = put("/api/cartelera", "{}");

        assertEquals(405, respuesta.estado());
        assertEquals("La ruta no acepta PUT", respuesta.error());
    }

    @Test
    void unCuerpoQueNoEsJsonEs400() {
        Respuesta respuesta = post("/api/salas", "{nombre:");

        assertEquals(400, respuesta.estado());
        assertEquals("El cuerpo del pedido no es un JSON válido", respuesta.error());
    }

    @Test
    void unPedidoSinCuerpoEs400YDiceQueFalta() {
        Respuesta sinCuerpo = post("/api/salas", null);
        Respuesta vacio = post("/api/salas", "");

        assertEquals(400, sinCuerpo.estado());
        assertEquals("Falta el cuerpo del pedido", sinCuerpo.error());
        assertEquals(400, vacio.estado());
        assertEquals("Falta el cuerpo del pedido", vacio.error());
    }

    @Test
    void unPedidoIncompletoEs400ConElMensajeDelPrimerCampoQueFalta() {
        Respuesta respuesta = post("/api/clientes", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("El nombre no puede estar vacío", respuesta.error());
    }

    @Test
    void unEmailSinFormatoEs400() {
        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana-sin-arroba\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El email tiene que tener la forma usuario@dominio.com", respuesta.error());
    }

    @Test
    void unaFuncionSinPeliculaNiSalaPideLaPeliculaPrimero() {
        Respuesta respuesta = post("/api/funciones", "{\"precio\":5000}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la película", respuesta.error());
    }

    @Test
    void elAltaDePeliculaSinDuracionEs400ConElMensajeDelGestorYLaEdicionParcialNoLaPide() {
        Respuesta alta = post("/api/peliculas", "{\"titulo\":\"Dune\",\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}");
        assertEquals(400, alta.estado());
        assertEquals("Falta la duración", alta.error());

        int id = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":155,"
                + "\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}").json().get("id").asInt();
        Respuesta edicion = put("/api/peliculas/" + id, "{\"titulo\":\"Dune: Parte Uno\"}");
        assertEquals(200, edicion.estado());
        assertEquals(155, edicion.json().get("duracionMinutos").asInt());
    }

    // Sin validar el largo, el INSERT lo rechaza la base y el usuario ve un 500 genérico.
    @Test
    void unTextoMasLargoQueSuColumnaEs400ConElLargoMaximo() {
        Respuesta pelicula = post("/api/peliculas", "{\"titulo\":\"" + "x".repeat(101) + "\","
                + "\"duracionMinutos\":90,\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}");
        assertEquals(400, pelicula.estado());
        assertEquals("El título no puede tener más de 100 caracteres", pelicula.error());

        Respuesta sala = post("/api/salas", "{\"nombre\":\"" + "x".repeat(51) + "\","
                + "\"tipo\":\"DOS_D\",\"butacasPorFila\":[5]}");
        assertEquals(400, sala.estado());
        assertEquals("El nombre no puede tener más de 50 caracteres", sala.error());

        Respuesta cliente = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@"
                + ("m".repeat(31) + ".").repeat(3) + "com\"}");
        assertEquals(400, cliente.estado());
        assertEquals("El email no puede tener más de 100 caracteres", cliente.error());
    }

    @Test
    void unTituloDeCienCaracteresEntraJusto() {
        Respuesta pelicula = post("/api/peliculas", "{\"titulo\":\"" + "x".repeat(100) + "\","
                + "\"duracionMinutos\":90,\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}");

        assertEquals(201, pelicula.estado());
    }

    @Test
    void unaReservaQueCambioMientrasSeProcesabaEs409() {
        var respuesta = new ManejadorErrores()
                .conflictoDeVersion(new ObjectOptimisticLockingFailureException(Reserva.class, 1));

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("La reserva cambió mientras se procesaba: volvé a intentarlo", respuesta.getBody().error());
    }

    @Test
    void unClienteConEmailRepetidoEs409() {
        post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Otra\",\"email\":\"ana@mail.com\"}");

        assertEquals(409, respuesta.estado());
        assertEquals("Ya existe un usuario con ese email", respuesta.error());
    }

    @Test
    void unaFuncionDeUnaPeliculaQueNoExisteEs404() {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":999,\"salaId\":1,"
                + "\"inicio\":\"2026-08-20T20:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la película 999", respuesta.error());
    }

    // La ruta tal como llegó: con la barra final recortada, /api/salas/ decía que no existe /api/salas.
    @Test
    void unaRutaQueNoExisteEs404YLaNombraComoLlego() {
        Respuesta respuesta = get("/api/no-existe");
        Respuesta conBarraFinal = get("/api/salas/");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la ruta /api/no-existe", respuesta.error());
        assertEquals(404, conBarraFinal.estado());
        assertEquals("No existe la ruta /api/salas/", conBarraFinal.error());
    }

    // Sin el Content-Type fijado, Spring negociaba el error contra el Accept y no podía escribirlo:
    // un 500 vacío con XML, la página Whitelabel con HTML.
    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "text/html"})
    void unErrorSaleEnJsonAunqueElClientePidaOtroFormato(String formato) {
        Respuesta respuesta = pedirAceptando("/api/no-existe", formato);

        assertEquals(404, respuesta.estado());
        assertEquals(MediaType.APPLICATION_JSON, respuesta.cabeceras().getContentType());
        assertEquals("No existe la ruta /api/no-existe", respuesta.error());
    }

    @Test
    void pedirUnaRespuestaQueNoSeaJsonEs406() {
        Respuesta respuesta = pedirAceptando("/api/cartelera", "application/xml");

        assertEquals(406, respuesta.estado());
        assertEquals(MediaType.APPLICATION_JSON, respuesta.cabeceras().getContentType());
        assertEquals("Esta API responde solo JSON", respuesta.error());
    }

    // Dos altas que pasan el mismo existsBy… y chocan en el UNIQUE: no es una base caída, es 409 y no 500.
    @Test
    void unAltaQueChocaConUnaRestriccionDeLaBaseEs409() {
        Respuesta respuesta = post(RutasDePrueba.RUTA_CHOQUE, null);

        assertEquals(409, respuesta.estado());
        assertEquals("Otro pedido cambió estos datos al mismo tiempo: recargá y volvé a intentarlo",
                respuesta.error());
    }

    // Un JSON bien formado con un tipo equivocado no es "JSON inválido": el mensaje nombra el campo.
    @Test
    void unTipoEquivocadoEnElCuerpoEs400YNombraElCampo() {
        Respuesta precio = post("/api/candy/productos", "{\"nombre\":\"Agua\",\"tipo\":\"BEBIDA\",\"precio\":\"abc\"}");
        Respuesta tarifa = post("/api/reservas", "{\"funcionId\":1,\"nombre\":\"Ana\",\"email\":\"ana@mail.com\","
                + "\"butacas\":{\"A1\":\"VIP\"}}");

        assertEquals(400, precio.estado());
        assertEquals("El campo precio tiene un valor inválido: abc", precio.error());
        assertEquals(400, tarifa.estado());
        assertEquals("El campo butacas.A1 tiene un valor inválido: VIP", tarifa.error());
    }

    @Test
    void unParametroDeLaQueryMalEscritoEs400YUnIdentificadorDeLaRutaEs404() {
        Respuesta query = get("/api/candy/productos?todos=quizas");
        Respuesta ruta = get("/api/funciones/abc");

        assertEquals(400, query.estado());
        assertEquals("El filtro todos tiene que ser true o false", query.error());
        assertEquals(404, ruta.estado());
        assertEquals("No existe la ruta /api/funciones/abc", ruta.error());
    }

    @Test
    void unaFechaMalFormadaEnLaQueryEs400ConElNombreDelDato() {
        assertEquals("La fecha no es válida: usá AAAA-MM-DD", get("/api/arqueo?fecha=ayer").error());
        assertEquals("El día no es válido: usá AAAA-MM-DD", get("/api/reservas?dia=13-08-2026").error());
        assertEquals("La fecha de inicio no es válida: usá AAAA-MM-DD", get("/api/funciones?desde=x").error());
        assertEquals(400, get("/api/candy/arqueo?fecha=2026-13-45").estado());
    }

    @Test
    void unNumeroOUnFiltroMalEscritoEnLaQueryEs400ConElNombreDelDato() {
        assertEquals("El id del cliente tiene que ser un número", get("/api/candy/compras?clienteId=abc").error());
        assertEquals("El id de la película tiene que ser un número", get("/api/funciones?peliculaId=x").error());
        assertEquals("El id de la sala tiene que ser un número", get("/api/programaciones?salaId=x").error());
        assertEquals("El filtro publicada tiene que ser true o false", get("/api/peliculas?publicada=quizas").error());
        assertEquals("El filtro activa tiene que ser true o false", get("/api/programaciones?activa=x").error());
    }

    @Test
    void unDiaDeLaSemanaInvalidoDiceLoMismoEnPromocionesYEnProgramaciones() {
        Respuesta promocion = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\","
                + "\"diasSemana\":[\"JUEVESITO\"]}");
        Respuesta programacion = post("/api/programaciones/previsualizacion", "{\"peliculaId\":1,\"salaId\":1,"
                + "\"desde\":\"2026-09-01\",\"horaInicio\":\"20:30\",\"diasSemana\":[\"JUEVESITO\"],"
                + "\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}");

        assertEquals(400, promocion.estado());
        assertEquals("Valor inválido para el día de la semana: JUEVESITO", promocion.error());
        assertEquals(400, programacion.estado());
        assertEquals(promocion.error(), programacion.error());
    }

    // Sin @Valid a propósito, porque es parcial: lo que no viaja queda igual, pero lo que viaja se valida.
    @Test
    void laEdicionParcialDePeliculaRechazaLoQueVieneMal() {
        int id = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":155,"
                + "\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}").json().get("id").asInt();
        String ruta = "/api/peliculas/" + id;

        assertEquals("El título no puede estar vacío", put(ruta, "{\"titulo\":\"  \"}").error());
        assertEquals("La duración tiene que ser mayor a cero", put(ruta, "{\"duracionMinutos\":0}").error());
        assertEquals("El puntaje tiene que estar entre 0 y 10", put(ruta, "{\"puntaje\":11}").error());
        assertEquals("Los votos no pueden ser negativos", put(ruta, "{\"votos\":-1}").error());
        assertEquals("El año tiene que estar entre 1895 y 2031", put(ruta, "{\"anio\":-3}").error());
        assertEquals(400, put(ruta, "{\"generos\":[]}").estado());
        assertEquals("Dune", get(ruta).json().get("titulo").asText(), "nada de lo rechazado se guardó");
        assertEquals(155, get(ruta).json().get("duracionMinutos").asInt());
    }

    @Test
    void validarUnaEntradaSinCodigoEs400YNo404() {
        Respuesta sinCampo = post("/api/acceso", "{}");
        Respuesta enBlanco = post("/api/acceso", "{\"codigo\":\"  \"}");

        assertEquals(400, sinCampo.estado());
        assertEquals("Falta el código de acceso", sinCampo.error());
        assertEquals(400, enBlanco.estado());
        assertEquals("Falta el código de acceso", enBlanco.error());
    }

    // Una fecha que pasó Parseo y desborda en la aritmética de un gestor la provocó el pedido: 400, no 500.
    @Test
    void unaFechaQueDesbordaEnUnGestorEs400() {
        var respuesta = new ManejadorErrores().fechaFueraDeRango(new DateTimeException("Invalid value for Year"));

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Una de las fechas del pedido no es válida", respuesta.getBody().error());
    }

    private Respuesta pedirAceptando(String ruta, String formato) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBasicAuth(EMAIL_ADMIN, CLAVE_ADMIN);
        cabeceras.setAccept(List.of(MediaType.parseMediaType(formato)));
        ResponseEntity<String> respuesta = cliente.exchange(URI.create(ruta), HttpMethod.GET,
                new HttpEntity<>(cabeceras), String.class);
        return new Respuesta(respuesta.getStatusCode().value(), respuesta.getBody(), respuesta.getHeaders());
    }
}
