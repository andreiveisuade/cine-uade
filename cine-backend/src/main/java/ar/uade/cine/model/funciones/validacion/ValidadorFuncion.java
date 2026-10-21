package ar.uade.cine.model.funciones.validacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Reglas del horario de una función, compartidas con programación y grilla; Fabricación pura.
public final class ValidadorFuncion {

    // A más de un año es un error de tipeo (2062 por 2026). En días y no en horas, para incluir el último día.
    private static final int ANIOS_DE_HORIZONTE = 1;

    private ValidadorFuncion() {
    }

    public static void inicio(LocalDateTime inicio) {
        Regla.objeto(inicio).obligatorio("Falta la fecha y hora de la función");
        sinSegundos(inicio.toLocalTime(), "La hora de la función");
    }

    // Una función empieza en un minuto exacto. El sujeto («La hora de apertura») arranca el mensaje.
    public static void sinSegundos(LocalTime hora, String sujeto) {
        if (hora != null && !hora.equals(hora.truncatedTo(ChronoUnit.MINUTES))) {
            throw new DatoInvalido(sujeto + " tiene que ir sin segundos");
        }
    }

    // El mensaje lo pone quien pregunta: la función dice cuándo empieza; programación y grilla, cuándo terminan.
    public static void dentroDelHorizonte(LocalDate dia, LocalDate hoy, String mensaje) {
        if (dia.isAfter(hoy.plusYears(ANIOS_DE_HORIZONTE))) {
            throw new DatoInvalido(mensaje);
        }
    }
}
