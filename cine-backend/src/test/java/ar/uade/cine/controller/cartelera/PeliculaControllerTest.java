package ar.uade.cine.controller.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.usuarios.GestorEmpleados;

class PeliculaControllerTest extends PruebaDeApi {

    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private GestorEmpleados empleados;
    @Autowired
    private PeliculaRepository peliculas;
    @Autowired
    private SalaRepository salas;
    @Autowired
    private FuncionRepository funciones;

    // El controller no busca antes: el 404 lo da el gestor, y esto evita que se vuelva un 400.
    @Test
    void editarOBorrarUnaPeliculaQueNoExisteEs404() {
        Respuesta editada = put("/api/peliculas/99", "{\"titulo\":\"Dune\"}");
        Respuesta borrada = pedirComo(HttpMethod.DELETE, "/api/peliculas/99", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(404, editada.estado());
        assertEquals("No existe la película 99", editada.error());
        assertEquals(404, borrada.estado());
        assertEquals("No existe la película 99", borrada.error());
    }

    // Lo que falta lo corta el pedido, con el mismo texto que daría la película.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            sin título       | {"duracionMinutos":155,"generos":["ACCION"],"clasificacion":"ATP"}             | Falta el título
            título en blanco | {"titulo":" ","duracionMinutos":155,"generos":["ACCION"],"clasificacion":"ATP"} | Falta el título
            sin géneros      | {"titulo":"Dune","duracionMinutos":155,"generos":[],"clasificacion":"ATP"}    | La película tiene que tener al menos un género
            """)
    void unAltaSinTituloOSinGenerosEs400ConElTextoDeLaPelicula(String caso, String cuerpo, String mensaje) {
        Respuesta respuesta = post("/api/peliculas", cuerpo);

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    // El pedido solo mira que venga: el rango lo pone la película, así el alta y la edición dicen lo mismo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            cero,                 0
            el entero más grande, 2147483647
            """)
    void unaDuracionFueraDeRangoEs400ConElTextoDeLaPelicula(String caso, int minutos) {
        Respuesta respuesta = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":" + minutos
                + ",\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("La duración tiene que estar entre 1 y 600 minutos", respuesta.error());
    }

    // Pública, pero lo no aprobado en el buzón no existe sin sesión de administrador (ni para el acomodador).
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            pendiente sin sesión,             PENDIENTE,  ,                    ,             404
            descartada sin sesión,            DESCARTADA, ,                    ,             404
            confirmada sin sesión,            CONFIRMADA, ,                    ,             200
            pendiente con el acomodador,      PENDIENTE,  puerta@cine.test,    clave-puerta, 404
            pendiente con el administrador,   PENDIENTE,  admin@prueba.test,   clave-de-prueba, 200
            descartada con el administrador,  DESCARTADA, admin@prueba.test,   clave-de-prueba, 200
            """)
    void elDetalleDeLoQueNoPasoElBuzonSoloLoVeElAdministrador(String caso, EstadoRevision estado, String email,
            String clave, int esperado) {
        empleados.registrar("Portero", "puerta@cine.test", "clave-puerta", Rol.ACOMODADOR);
        int id = importadaEn(estado);

        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/peliculas/" + id, null, email, clave);

        assertEquals(esperado, respuesta.estado());
        if (esperado == 404) {
            assertEquals("No existe la película " + id, respuesta.error());
        } else {
            assertEquals("Dune", respuesta.json().get("titulo").asText());
        }
    }

    // Mismo criterio que el detalle: no revela qué ids trajo el importador.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            pendiente sin sesión,            PENDIENTE,  ,                  ,                404
            descartada sin sesión,           DESCARTADA, ,                  ,                404
            confirmada sin sesión,           CONFIRMADA, ,                  ,                200
            pendiente con el administrador,  PENDIENTE,  admin@prueba.test, clave-de-prueba, 200
            descartada con el administrador, DESCARTADA, admin@prueba.test, clave-de-prueba, 200
            """)
    void lasFuncionesDeLoQueNoPasoElBuzonSoloLasVeElAdministrador(String caso, EstadoRevision estado,
            String email, String clave, int esperado) {
        int id = importadaEn(estado);

        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/peliculas/" + id + "/funciones", null, email, clave);

        assertEquals(esperado, respuesta.estado());
        if (esperado == 404) {
            assertEquals("No existe la película " + id, respuesta.error());
        } else {
            assertEquals(0, respuesta.json().size());
        }
    }

    // R31: los formatos viajan con el nombre de la constante, en orden, y al menos uno.
    @Test
    void losFormatosViajanConElNombreDeLaConstante() {
        Respuesta alta = post("/api/peliculas", "{\"titulo\":\"Avatar\",\"duracionMinutos\":162,"
                + "\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\",\"formatos\":[\"TRES_D\",\"DOS_D\"]}");
        int id = alta.json().get("id").asInt();

        assertEquals(201, alta.estado());
        assertEquals("[\"DOS_D\",\"TRES_D\"]", get("/api/peliculas/" + id).json().get("formatos").toString());
        Respuesta sinFormatos = put("/api/peliculas/" + id, "{\"formatos\":[]}");
        assertEquals(400, sinFormatos.estado());
        assertEquals("La película tiene que tener al menos un formato", sinFormatos.error());
    }

    // Se manda la URL que se copia del navegador y se guarda el id, que es lo que embebe la web.
    @Test
    void elAltaConLaUrlDelTrailerGuardaElIdDelVideo() {
        Respuesta respuesta = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":155,"
                + "\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\","
                + "\"trailerYoutube\":\"https://www.youtube.com/watch?v=Way9Dexny3w\"}");

        assertEquals(201, respuesta.estado());
        assertEquals("Way9Dexny3w", respuesta.json().get("trailerYoutube").asText());
    }

    // Parcial como el resto: sin el campo queda el que estaba, vacío lo saca.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            sin el campo queda el anterior | {"titulo":"Dune"}                                      | 200 | Way9Dexny3w
            otro id lo reemplaza           | {"trailerYoutube":"https://youtu.be/n9xhJrPXop4"}      | 200 | n9xhJrPXop4
            vacío lo saca                  | {"trailerYoutube":""}                                  | 200 |
            otro sitio es 400              | {"trailerYoutube":"https://vimeo.com/123"}             | 400 | Way9Dexny3w
            """)
    void editarElTrailer(String caso, String cuerpo, int estado, String queda) {
        int id = revision.importar(DatosPelicula.con().titulo("Dune").duracion(155).generos(List.of(Genero.ACCION))
                .clasificacion(Clasificacion.ATP).trailerYoutube("Way9Dexny3w").armar()).getId();

        Respuesta respuesta = put("/api/peliculas/" + id, cuerpo);

        assertEquals(estado, respuesta.estado());
        if (estado == 400) {
            assertEquals("El trailer tiene que ser un video de YouTube", respuesta.error());
        }
        assertEquals(queda, revision.listarPendientes().get(0).getTrailerYoutube());
    }

    private int importadaEn(EstadoRevision estado) {
        int id = revision.importar(DatosPelicula.deAlta("Dune", 155, List.of(Genero.ACCION), Clasificacion.ATP))
                .getId();
        switch (estado) {
            case CONFIRMADA -> revision.confirmar(id);
            case DESCARTADA -> revision.descartar(id);
            case PENDIENTE -> { }
        }
        return id;
    }

    @Test
    void publicarUnaPendienteEs400YSigueSinPublicar() {
        int id = revision.importar(DatosPelicula.deAlta("Dune", 155, List.of(Genero.ACCION), Clasificacion.ATP))
                .getId();

        Respuesta respuesta = put("/api/peliculas/" + id, "{\"enCartelera\":true}");

        assertEquals(400, respuesta.estado());
        assertEquals("La película Dune no está confirmada: revisala antes de publicarla", respuesta.error());
        assertFalse(get("/api/peliculas/" + id).json().get("enCartelera").asBoolean());
    }

    // El tipo sale de lo que se vende: Alien solo pasó en 3D e IMAX, y una película entra en todos los tipos que tenga.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            2D                       | tipo=DOS_D                | [Dune, Interstellar, Alien]
            3D                       | tipo=TRES_D               | [Avatar]
            IMAX                     | tipo=IMAX                 | [Interstellar]
            sin distinguir mayúsculas| tipo=imax                 | [Interstellar]
            combinado con género     | genero=DRAMA&tipo=TRES_D  | [Avatar]
            género sin ese tipo      | genero=ACCION&tipo=TRES_D | []
            tipo vacío no filtra     | tipo=                     | [Dune, Avatar, Interstellar, Alien]
            """)
    void laCarteleraFiltraPorTipoSegunLasFuncionesPorDelante(String caso, String consulta, String titulos) {
        Sala comun = salas.save(new Sala("Sala 2D", TipoSala.DOS_D, 15));
        Sala tresD = salas.save(new Sala("Sala 3D", TipoSala.TRES_D, 15));
        Sala imax = salas.save(new Sala("Sala IMAX", TipoSala.IMAX, 15));
        LocalDateTime proxima = reloj.ahora().plusDays(2);
        funcion(enCartelera("Dune", Genero.ACCION), comun, proxima, Proyeccion.DOS_D);
        funcion(enCartelera("Avatar", Genero.DRAMA), tresD, proxima, Proyeccion.TRES_D);
        funcion(enCartelera("Interstellar", Genero.ACCION), imax, proxima, Proyeccion.DOS_D);
        Pelicula alien = enCartelera("Alien", Genero.ACCION);
        funcion(alien, comun, proxima.plusHours(4), Proyeccion.DOS_D);
        funcion(alien, imax, reloj.ahora().minusDays(1), Proyeccion.TRES_D);

        Respuesta respuesta = get("/api/cartelera?" + consulta);

        assertEquals(200, respuesta.estado());
        assertEquals(titulos, StreamSupport.stream(respuesta.json().spliterator(), false)
                .map(p -> p.get("titulo").asText()).toList().toString());
    }

    // CUATRO_D es un tipo de sala válido, pero no un tipo por el que se filtra.
    @ParameterizedTest
    @CsvSource(textBlock = """
            CUATRO_D
            CINE
            """)
    void unTipoQueNoExisteEs400(String tipo) {
        Respuesta respuesta = get("/api/cartelera?tipo=" + tipo);

        assertEquals(400, respuesta.estado());
        assertEquals("El tipo de película no es válido: " + tipo, respuesta.error());
    }

    private Pelicula enCartelera(String titulo, Genero genero) {
        int id = post("/api/peliculas", "{\"titulo\":\"" + titulo + "\",\"duracionMinutos\":120,\"generos\":[\""
                + genero + "\"],\"clasificacion\":\"ATP\",\"formatos\":[\"DOS_D\",\"TRES_D\"]}")
                .json().get("id").asInt();
        return peliculas.findById(id).orElseThrow();
    }

    private void funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, Proyeccion proyeccion) {
        funciones.save(new Funcion(pelicula, sala, inicio,
                new FormatoDeFuncion(Version.SUBTITULADA, proyeccion, Dinero.de(5000))));
    }
}
