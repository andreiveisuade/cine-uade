package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.TipoTarifa;

class ParseoTest {

    @Test
    void leeLaConstanteDelEnum() {
        assertEquals(MedioPago.EFECTIVO, Parseo.constante(MedioPago.class, "EFECTIVO", "el medio"));
        assertEquals(TipoTarifa.JUBILADO, Parseo.constante(TipoTarifa.class, "JUBILADO", "la tarifa"));
    }

    @Test
    void toleraMinusculasYEspaciosAlrededor() {
        assertEquals(MedioPago.EFECTIVO, Parseo.constante(MedioPago.class, "  efectivo ", "el medio"));
        assertEquals(Genero.ACCION, Parseo.constante(Genero.class, "Accion", "el género"));
    }

    @Test
    void unValorQueNoExisteEsUnError() {
        Rechazo e = assertThrows(Rechazo.class,
                () -> Parseo.constante(MedioPago.class, "BITCOIN", "el medio de pago"));

        assertTrue(e.getMessage().contains("el medio de pago"), "el mensaje no dice qué campo falló");
        assertTrue(e.getMessage().contains("BITCOIN"), "el mensaje no dice qué valor llegó");
    }

    @Test
    void elMensajeNoDejaAsomarNombresDeClases() {
        Rechazo e = assertThrows(Rechazo.class,
                () -> Parseo.constante(MedioPago.class, "BITCOIN", "el medio de pago"));

        assertTrue(e.getMessage().contains("Valor inválido"), "no es el mensaje de esta capa");
        assertTrue(!e.getMessage().contains("ar.uade.cine"), "se filtró el paquete al usuario");
        assertTrue(!e.getMessage().contains("No enum constant"), "se filtró el mensaje de Java");
    }

    @Test
    void faltarElValorTambienEsUnError() {
        assertThrows(Rechazo.class,
                () -> Parseo.constante(MedioPago.class, null, "el medio"));
        assertThrows(Rechazo.class,
                () -> Parseo.constante(MedioPago.class, "   ", "el medio"));
    }

    @Test
    void leeUnaListaDeConstantes() {
        assertEquals(List.of(Genero.ACCION, Genero.DRAMA),
                Parseo.constantes(Genero.class, List.of("ACCION", "DRAMA"), "el género"));
    }

    @Test
    void unaListaAusenteQuedaVacia() {
        assertEquals(List.of(), Parseo.constantes(Genero.class, null, "el género"));
    }

    @Test
    void unaListaConUnValorInvalidoFallaEntera() {
        assertThrows(Rechazo.class,
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

    // El mensaje llega tal cual al usuario: arranca en mayúscula y dice cómo escribirlo bien.
    @Test
    void unaFechaOUnaHoraMalEscritaDiceElFormatoQueEspera() {
        assertEquals("La fecha y hora de la función no es válida: usá AAAA-MM-DDTHH:MM",
                mensaje(() -> Parseo.momento("20/08/2026", "la fecha y hora de la función")));
        assertEquals("La fecha de inicio no es válida: usá AAAA-MM-DD",
                mensaje(() -> Parseo.dia("ayer", "la fecha de inicio")));
        assertEquals("La hora de apertura no es válida: usá HH:MM",
                mensaje(() -> Parseo.hora("25:00", "la hora de apertura")));
    }

    @Test
    void elAdjetivoConcuerdaConElDatoQueFallo() {
        assertEquals("El inicio de la vigencia no es válido: usá AAAA-MM-DD",
                mensaje(() -> Parseo.dia("2026-13-45", "el inicio de la vigencia")));
    }

    @Test
    void unNumeroOUnFiltroMalEscritoArrancaEnMayuscula() {
        assertEquals("El id del cliente tiene que ser un número",
                mensaje(() -> Parseo.numeroOpcional("abc", "el id del cliente")));
        assertEquals("El filtro publicada tiene que ser true o false",
                mensaje(() -> Parseo.booleanOpcional("quizas", "el filtro publicada")));
    }

    @Test
    void unaConstanteQueNoExisteDiceQueValorLlego() {
        assertEquals("Valor inválido para el idioma: KLINGON",
                mensaje(() -> Parseo.constante(Version.class, "KLINGON", "el idioma")));
        assertEquals("Falta el idioma", mensaje(() -> Parseo.constante(Version.class, " ", "el idioma")));
    }

    @Test
    void unDiaQueNoExisteEnElCalendarioSeRechaza() {
        assertThrows(Rechazo.class, () -> Parseo.dia("2026-02-30", "la fecha"));
        assertThrows(Rechazo.class, () -> Parseo.hora("25:00", "la hora"));
    }

    @Test
    void faltarLaFechaEsUnErrorDistintoAQueEsteMalEscrita() {
        assertTrue(assertThrows(Rechazo.class,
                () -> Parseo.dia(null, "la fecha")).getMessage().contains("Falta"));
        assertTrue(assertThrows(Rechazo.class,
                () -> Parseo.dia("ayer", "la fecha")).getMessage().contains("válida"));
    }

    private static String mensaje(Executable accion) {
        return assertThrows(Rechazo.class, accion).getMessage();
    }
}
