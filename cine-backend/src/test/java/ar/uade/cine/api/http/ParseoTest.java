package ar.uade.cine.api.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.TipoTarifa;

/**
 * Lo que entra por HTTP es texto y nadie garantiza que sea el que esperamos. Estas
 * pruebas fijan las dos mitades del contrato: qué se acepta y, sobre todo, qué dice el
 * mensaje cuando se rechaza —porque ese texto lo lee el usuario, no un programador—.
 */
class ParseoTest {

    @Test
    void leeLaConstanteDelEnum() {
        assertEquals(MedioPago.EFECTIVO, Parseo.constante(MedioPago.class, "EFECTIVO", "el medio"));
        assertEquals(TipoTarifa.JUBILADO, Parseo.constante(TipoTarifa.class, "JUBILADO", "la tarifa"));
    }

    /** El front manda lo que el usuario eligió; los espacios y las minúsculas no son un error. */
    @Test
    void toleraMinusculasYEspaciosAlrededor() {
        assertEquals(MedioPago.EFECTIVO, Parseo.constante(MedioPago.class, "  efectivo ", "el medio"));
        assertEquals(Genero.ACCION, Parseo.constante(Genero.class, "Accion", "el género"));
    }

    @Test
    void unValorQueNoExisteEsUnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parseo.constante(MedioPago.class, "BITCOIN", "el medio de pago"));

        assertTrue(e.getMessage().contains("el medio de pago"), "el mensaje no dice qué campo falló");
        assertTrue(e.getMessage().contains("BITCOIN"), "el mensaje no dice qué valor llegó");
    }

    /**
     * El mensaje es para el usuario: no puede filtrar nombres de clases de Java. El
     * IllegalArgumentException de Enum.valueOf dice "No enum constant ar.uade.cine..." y
     * eso es exactamente lo que esta capa está para tapar.
     */
    @Test
    void elMensajeNoDejaAsomarNombresDeClases() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parseo.constante(MedioPago.class, "BITCOIN", "el medio de pago"));

        assertTrue(e.getMessage().contains("Valor inválido"), "no es el mensaje de esta capa");
        assertTrue(!e.getMessage().contains("ar.uade.cine"), "se filtró el paquete al usuario");
        assertTrue(!e.getMessage().contains("No enum constant"), "se filtró el mensaje de Java");
    }

    @Test
    void faltarElValorTambienEsUnError() {
        assertThrows(IllegalArgumentException.class,
                () -> Parseo.constante(MedioPago.class, null, "el medio"));
        assertThrows(IllegalArgumentException.class,
                () -> Parseo.constante(MedioPago.class, "   ", "el medio"));
    }

    @Test
    void leeUnaListaDeConstantes() {
        assertEquals(List.of(Genero.ACCION, Genero.DRAMA),
                Parseo.constantes(Genero.class, List.of("ACCION", "DRAMA"), "el género"));
    }

    /**
     * Una lista ausente no es una lista con errores: la película sin géneros la rechaza
     * el gestor con su propio mensaje, que es donde vive esa regla.
     */
    @Test
    void unaListaAusenteQuedaVacia() {
        assertEquals(List.of(), Parseo.constantes(Genero.class, null, "el género"));
    }

    @Test
    void unaListaConUnValorInvalidoFallaEntera() {
        assertThrows(IllegalArgumentException.class,
                () -> Parseo.constantes(Genero.class, List.of("ACCION", "MUSICAL_INVENTADO"), "el género"));
    }

    @Test
    void leeElMomentoDeUnaFuncion() {
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 30),
                Parseo.momento("2026-08-20T20:30:00", "el inicio"));
    }

    @Test
    void leeElDiaYLaHoraPorSeparado() {
        assertEquals(LocalDate.of(2026, 8, 20), Parseo.dia("2026-08-20", "la fecha"));
        assertEquals(LocalTime.of(20, 30), Parseo.hora("20:30", "la hora"));
    }

    @Test
    void unaFechaMalEscritaLoDiceEnCastellano() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parseo.momento("20/08/2026", "el inicio de la función"));

        assertTrue(e.getMessage().contains("el inicio de la función"));
        assertTrue(!e.getMessage().contains("DateTimeParseException"), "se filtró la excepción de Java");
    }

    @Test
    void unDiaQueNoExisteEnElCalendarioSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> Parseo.dia("2026-02-30", "la fecha"));
        assertThrows(IllegalArgumentException.class, () -> Parseo.hora("25:00", "la hora"));
    }

    @Test
    void faltarLaFechaEsUnErrorDistintoAQueEsteMalEscrita() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Parseo.dia(null, "la fecha")).getMessage().contains("Falta"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Parseo.dia("ayer", "la fecha")).getMessage().contains("válida"));
    }
}
