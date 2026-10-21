package ar.uade.cine.controller.http.cuerpo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// Cada eslabón por separado: contesta lo suyo y pasa lo ajeno. El orden lo cubre ManejadorErroresTest.
class LecturaDelCuerpoTest {

    private static final Throwable SIN_CAUSA = null;

    @Test
    void sinCuerpoContestaSoloCuandoNoHayCausa() {
        assertEquals(Optional.of("Falta el cuerpo del pedido"), new SinCuerpo().mensaje(ilegible(SIN_CAUSA)));
        assertEquals(Optional.empty(), new SinCuerpo().mensaje(ilegible(jsonRoto())));
    }

    @Test
    void campoConValorInvalidoNombraElCampoYElValor() {
        InvalidFormatException formato = InvalidFormatException.from(null, "no es un número", "abc", Integer.class);
        formato.prependPath(new Object(), "precio");

        assertEquals(Optional.of("El campo precio tiene un valor inválido: abc"),
                new CampoConValorInvalido().mensaje(ilegible(formato)));
    }

    @Test
    void campoConValorInvalidoUneElCaminoConPuntosYUsaElIndiceDeUnArray() {
        JsonMappingException mapeo = JsonMappingException.from((com.fasterxml.jackson.core.JsonParser) null, "x");
        mapeo.prependPath(new Object(), 2);
        mapeo.prependPath(new Object(), "items");

        assertEquals(Optional.of("El campo items.2 tiene un valor inválido"),
                new CampoConValorInvalido().mensaje(ilegible(mapeo)));
    }

    @Test
    void campoConValorInvalidoPasaSiNoHayCampo() {
        assertEquals(Optional.empty(), new CampoConValorInvalido().mensaje(ilegible(raizEquivocada())));
        assertEquals(Optional.empty(), new CampoConValorInvalido().mensaje(ilegible(jsonRoto())));
    }

    @Test
    void raizQueNoEsObjetoContestaAnteUnTipoEquivocadoSinCampo() {
        assertEquals(Optional.of("El cuerpo del pedido tiene que ser un objeto JSON"),
                new RaizQueNoEsObjeto().mensaje(ilegible(raizEquivocada())));
        assertEquals(Optional.empty(), new RaizQueNoEsObjeto().mensaje(ilegible(jsonRoto())));
    }

    @Test
    void jsonMalFormadoContestaSiempre() {
        assertEquals(Optional.of("El cuerpo del pedido no es un JSON válido"),
                new JsonMalFormado().mensaje(ilegible(jsonRoto())));
    }

    private static HttpMessageNotReadableException ilegible(Throwable causa) {
        return new HttpMessageNotReadableException("ilegible", causa, mock(HttpInputMessage.class));
    }

    private static JsonParseException jsonRoto() {
        return new JsonParseException(null, "Unexpected character");
    }

    private static MismatchedInputException raizEquivocada() {
        return MismatchedInputException.from(null, Object.class, "Cannot deserialize from Array value");
    }
}
