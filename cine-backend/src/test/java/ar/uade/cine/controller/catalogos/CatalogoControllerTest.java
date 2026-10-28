package ar.uade.cine.controller.catalogos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;

class CatalogoControllerTest extends PruebaDeApi {

    // Sin credenciales, como los demás catálogos: el cliente también los lee.
    private Respuesta getPublico(String ruta) {
        return pedirComo(HttpMethod.GET, ruta, null, null, null);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            géneros,            /api/generos
            clasificaciones,    /api/clasificaciones
            tipos de sala,      /api/tipos-sala
            idiomas,            /api/idiomas
            proyecciones,       /api/proyecciones
            medios de pago,     /api/medios-pago
            tarifas,            /api/tarifas
            tipos de producto,  /api/tipos-producto
            tipos de promoción, /api/tipos-promocion
            """)
    void cadaCatalogoSeLeeSinCredenciales(String caso, String ruta) {
        assertEquals(200, getPublico(ruta).estado());
    }

    @Test
    void losGenerosIdiomasYProyeccionesSonLasConstantes() {
        assertEquals("[\"ACCION\",\"COMEDIA\",\"DRAMA\",\"TERROR\",\"CIENCIA_FICCION\",\"ANIMACION\","
                + "\"DOCUMENTAL\",\"ROMANCE\",\"SUSPENSO\"]", getPublico("/api/generos").cuerpo());
        assertEquals("[\"DOBLADA\",\"SUBTITULADA\"]", getPublico("/api/idiomas").cuerpo());
        assertEquals("[\"DOS_D\",\"TRES_D\"]", getPublico("/api/proyecciones").cuerpo());
    }

    // Los flags no se deducen del nombre de la constante: los clientes los leen de acá.
    @Test
    void lasClasificacionesDicenLaEdadMinima() {
        assertEquals("[{\"nombre\":\"ATP\",\"edadMinima\":0},{\"nombre\":\"MAS_13\",\"edadMinima\":13},"
                + "{\"nombre\":\"MAS_16\",\"edadMinima\":16},{\"nombre\":\"MAS_18\",\"edadMinima\":18}]",
                getPublico("/api/clasificaciones").cuerpo());
    }

    @Test
    void losTiposDeSalaDicenSuRecargoYSiProyectanEn3D() {
        assertEquals("[{\"nombre\":\"DOS_D\",\"multiplicador\":1.0,\"soportaTresD\":false},"
                + "{\"nombre\":\"TRES_D\",\"multiplicador\":1.3,\"soportaTresD\":true},"
                + "{\"nombre\":\"IMAX\",\"multiplicador\":1.6,\"soportaTresD\":true},"
                + "{\"nombre\":\"CUATRO_D\",\"multiplicador\":1.8,\"soportaTresD\":true}]",
                getPublico("/api/tipos-sala").cuerpo());
    }

    @Test
    void losMediosDePagoDicenCualesPidenCodigoDeAutorizacion() {
        assertEquals("[{\"nombre\":\"EFECTIVO\",\"requiereAutorizacion\":false},"
                + "{\"nombre\":\"DEBITO\",\"requiereAutorizacion\":true},"
                + "{\"nombre\":\"CREDITO\",\"requiereAutorizacion\":true},"
                + "{\"nombre\":\"QR\",\"requiereAutorizacion\":true},"
                + "{\"nombre\":\"TRANSFERENCIA\",\"requiereAutorizacion\":true}]",
                getPublico("/api/medios-pago").cuerpo());
    }

    @Test
    void lasTarifasDicenSuDescuentoYCualesSeAcreditan() {
        assertEquals("[{\"nombre\":\"GENERAL\",\"multiplicador\":1.0,\"requiereAcreditacion\":false},"
                + "{\"nombre\":\"MENOR\",\"multiplicador\":0.6,\"requiereAcreditacion\":true},"
                + "{\"nombre\":\"JUBILADO\",\"multiplicador\":0.5,\"requiereAcreditacion\":true},"
                + "{\"nombre\":\"ESTUDIANTE\",\"multiplicador\":0.7,\"requiereAcreditacion\":true}]",
                getPublico("/api/tarifas").cuerpo());
    }

    @Test
    void losTiposDeProductoDicenCualEsElCombo() {
        Respuesta respuesta = getPublico("/api/tipos-producto");

        assertEquals(200, respuesta.estado());
        assertEquals("[{\"nombre\":\"POCHOCLOS\",\"esCombo\":false},{\"nombre\":\"BEBIDA\",\"esCombo\":false},"
                + "{\"nombre\":\"GOLOSINA\",\"esCombo\":false},{\"nombre\":\"COMBO\",\"esCombo\":true}]",
                respuesta.cuerpo());
    }

    @Test
    void losTiposDePromocionDicenQueCamposPideCadaUno() {
        Respuesta respuesta = getPublico("/api/tipos-promocion");

        assertEquals(200, respuesta.estado());
        assertEquals("[{\"nombre\":\"PORCENTAJE\",\"campos\":[\"porcentaje\"]},"
                + "{\"nombre\":\"MONTO_FIJO\",\"campos\":[\"monto\"]},"
                + "{\"nombre\":\"NXM\",\"campos\":[\"lleva\",\"paga\"]}]",
                respuesta.cuerpo());
    }

    // El mensaje del gestor sale de la misma lista que publica el catálogo.
    @Test
    void unaPromocionSinElCampoDeSuTipoNombraElQueFalta() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"2x1\",\"tipo\":\"NXM\",\"lleva\":2,"
                + "\"vigenciaDesde\":\"2026-08-01\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta cuántas entradas paga", respuesta.error());
    }
}
