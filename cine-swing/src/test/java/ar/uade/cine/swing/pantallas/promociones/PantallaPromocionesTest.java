package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El alta y la edición de una promoción desde la pantalla: qué manda según el tipo, qué rechaza antes y qué muestra.
class PantallaPromocionesTest {

    private static final String DOS_POR_UNO = "{\"id\":3,\"nombre\":\"2x1 miércoles\",\"tipo\":\"NXM\",\"lleva\":2,"
            + "\"paga\":1,\"vigenciaDesde\":\"2026-01-01\",\"vigenciaHasta\":\"2026-12-31\","
            + "\"diasSemana\":[\"WEDNESDAY\"],\"horaDesde\":\"18:00:00\",\"horaHasta\":null,"
            + "\"mediosPago\":[\"EFECTIVO\"],\"activa\":true}";
    private static final String QUINCE_OFF = "{\"id\":4,\"nombre\":\"Jubilados\",\"tipo\":\"PORCENTAJE\","
            + "\"porcentaje\":15.0,\"vigenciaDesde\":\"2026-01-01\",\"vigenciaHasta\":\"2026-06-30\",\"activa\":false}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/tipos-promocion", 200, "[{\"nombre\":\"PORCENTAJE\",\"campos\":[\"porcentaje\"]},"
                + "{\"nombre\":\"MONTO_FIJO\",\"campos\":[\"monto\"]},{\"nombre\":\"NXM\",\"campos\":[\"lleva\",\"paga\"]}]");
        servidor.responder("GET /api/promociones", 200, "[" + DOS_POR_UNO + "," + QUINCE_OFF + "]");
        servidor.responder("GET /api/medios-pago", 200,
                "[{\"nombre\":\"EFECTIVO\",\"requiereAutorizacion\":false},"
                        + "{\"nombre\":\"CREDITO\",\"requiereAutorizacion\":true}]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaPromociones(new ApiCatalogos(http), new ApiPromociones(http),
                new ApiImagenes(http)));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        encargado.cerrar();
        servidor.bajar();
    }

    @Test
    void listaCadaPromocionConSuBeneficioYCuandoCorre() {
        encargado.esperarTexto("1 activas de 2");

        assertEquals("2x1", encargado.celda(0, "Beneficio"));
        assertEquals("Mié · 18:00–23:59 · Efectivo", encargado.celda(0, "Cuándo").replace(", ", " · "));
        assertEquals("15% off", encargado.celda(1, "Beneficio"));
        assertEquals("todos los días, cualquier medio", encargado.celda(1, "Cuándo"));
        assertEquals("Dada de baja", encargado.celda(1, "Estado"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "porcentaje | Porcentaje | Porcentaje         | 20  | \"porcentaje\":20.0,\"monto\":null,\"lleva\":null,\"paga\":null",
            "monto fijo | Monto fijo | Monto a descontar  | 1500 | \"porcentaje\":null,\"monto\":1500.0,\"lleva\":null,\"paga\":null"})
    void creaConSoloElBeneficioDelTipo(String caso, String tipo, String campo, String valor, String beneficio) {
        servidor.responder("POST /api/promociones", 201, QUINCE_OFF);
        encargado.tipear("Nombre", "Martes")
                .elegir("Tipo", tipo)
                .tipear(campo, valor)
                .fecha("Desde", LocalDate.of(2026, 11, 1))
                .fecha("Hasta", LocalDate.of(2026, 11, 30))
                .tildar("Mar")
                .tipear("Desde hora", "9:30")
                .tildar("Crédito")
                .clic("Crear promoción");

        encargado.esperarTexto("Promoción creada");
        String cuerpo = servidor.ultimo("POST /api/promociones").cuerpo();
        assertTrue(cuerpo.contains(beneficio), cuerpo);
        assertTrue(cuerpo.contains("\"vigenciaDesde\":\"2026-11-01\",\"vigenciaHasta\":\"2026-11-30\","
                + "\"diasSemana\":[\"TUESDAY\"],\"horaDesde\":\"09:30\",\"horaHasta\":null,"
                + "\"mediosPago\":[\"CREDITO\"],\"imagenUrl\":null"), cuerpo);
        assertEquals("", encargado.valor("Nombre"), "después de crear el formulario queda vacío");
    }

    @Test
    void loQueFaltaOEstaMalEscritoNoSeManda() {
        encargado.tipear("Nombre", " ")
                .tipear("Desde hora", "25:00")
                .clic("Crear promoción");

        encargado.esperarTexto("Falta completar «Nombre»");
        encargado.esperarTexto("Falta completar «Hasta»");
        encargado.esperarTexto("«Desde hora»: «25:00» no es una hora (HH:mm)");
        assertEquals(0, servidor.veces("POST /api/promociones"));
    }

    @Test
    void editarPrecargaYGuardaSobreLaMisma() {
        servidor.responder("PUT /api/promociones/4", 200, QUINCE_OFF);
        encargado.elegirFila(1).clic("Editar");

        assertTrue(encargado.textos().contains("Editar Jubilados"));
        assertEquals("15", encargado.valor("Porcentaje"));
        assertEquals("2026-06-30", encargado.valor("Hasta"));
        assertFalse(encargado.habilitado("Tipo"), "el backend no deja cambiar el tipo");

        encargado.tipear("Porcentaje", "12,5").tildar("Sáb").clic("Guardar cambios");

        encargado.esperarTexto("Cambios guardados");
        String cuerpo = servidor.ultimo("PUT /api/promociones/4").cuerpo();
        assertTrue(cuerpo.contains("\"tipo\":\"PORCENTAJE\",\"porcentaje\":12.5,\"monto\":null"), cuerpo);
        assertTrue(cuerpo.contains("\"vigenciaHasta\":\"2026-06-30\",\"diasSemana\":[\"SATURDAY\"]"), cuerpo);
    }

    @Test
    void elegirUnaPromocionDeNxMMuestraSusDosCampos() {
        encargado.elegirFila(0).clic("Editar");

        assertEquals("2", encargado.valor("Lleva"));
        assertEquals("1", encargado.valor("Paga"));
        assertEquals("18:00", encargado.valor("Desde hora"));
    }

    @Test
    void cancelarLaEdicionVuelveAlAlta() {
        encargado.elegirFila(1).clic("Editar");
        assertFalse(encargado.hayBoton("Crear promoción"));

        encargado.clic("Cancelar");

        assertTrue(encargado.hayBoton("Crear promoción"));
        assertEquals("", encargado.valor("Nombre"));
    }

    @Test
    void elRechazoDelBackendSeMuestraTalCual() {
        servidor.responder("POST /api/promociones", 400,
                "{\"error\":\"La vigencia termina antes de empezar\"}");
        encargado.tipear("Nombre", "Martes")
                .fecha("Hasta", LocalDate.of(2026, 12, 31))
                .clic("Crear promoción");

        encargado.esperarTexto("La vigencia termina antes de empezar");
        assertEquals("Martes", encargado.valor("Nombre"), "el formulario no se pierde");
    }
}
