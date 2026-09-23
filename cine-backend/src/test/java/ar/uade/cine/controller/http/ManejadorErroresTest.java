package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;

class ManejadorErroresTest extends PruebaDeApi {

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
    void unPedidoIncompletoEs400ConElMensajeDelPrimerCampoQueFalta() {
        Respuesta respuesta = post("/api/clientes", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("El nombre no puede estar vacío", respuesta.error());
    }

    @Test
    void unEmailSinFormatoEs400() {
        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana-sin-arroba\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El email no es válido", respuesta.error());
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
        assertEquals("La duración debe ser mayor a cero", alta.error());

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
    void unClienteConEmailRepetidoEs409() {
        post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Otra\",\"email\":\"ana@mail.com\"}");

        assertEquals(409, respuesta.estado());
        assertEquals("Ya hay un cliente registrado con ese email", respuesta.error());
    }

    @Test
    void unaFuncionDeUnaPeliculaQueNoExisteEs404() {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":999,\"salaId\":1,"
                + "\"inicio\":\"2026-08-20T20:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la película 999", respuesta.error());
    }

    @Test
    void unaRutaQueNoExisteEs404() {
        Respuesta respuesta = get("/api/no-existe");

        assertEquals(404, respuesta.estado());
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
        assertEquals("El parámetro todos no es válido: quizas", query.error());
        assertEquals(404, ruta.estado());
    }

    @Test
    void unaFechaMalFormadaEnLaQueryEs400ConElNombreDelDato() {
        assertEquals("la fecha tiene que ser una fecha válida", get("/api/arqueo?fecha=ayer").error());
        assertEquals("el día tiene que ser una fecha válida", get("/api/reservas?dia=13-08-2026").error());
        assertEquals("la fecha de inicio tiene que ser una fecha válida", get("/api/funciones?desde=x").error());
        assertEquals(400, get("/api/candy/arqueo?fecha=2026-13-45").estado());
    }

    // Sin @Valid a propósito, porque es parcial: lo que no viaja queda igual, pero lo que viaja se valida.
    @Test
    void laEdicionParcialDePeliculaRechazaLoQueVieneMal() {
        int id = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":155,"
                + "\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}").json().get("id").asInt();
        String ruta = "/api/peliculas/" + id;

        assertEquals("El título no puede estar vacío", put(ruta, "{\"titulo\":\"  \"}").error());
        assertEquals("La duración debe ser mayor a cero", put(ruta, "{\"duracionMinutos\":0}").error());
        assertEquals("El puntaje va de 0 a 10", put(ruta, "{\"puntaje\":11}").error());
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
}
