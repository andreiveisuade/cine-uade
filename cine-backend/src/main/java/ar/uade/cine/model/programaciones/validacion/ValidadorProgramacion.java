package ar.uade.cine.model.programaciones.validacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

import ar.uade.cine.model.funciones.validacion.ValidadorFuncion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.validacion.Regla;

// Reglas del período, la hora y los días de una programación; Fabricación pura que llama Programacion.
// El formato y el precio no están acá: son los de una función, y los valida ValidadorFuncion.
public final class ValidadorProgramacion {

    // Un año, bisiesto incluido. Una programación cerrada genera todo su rango en una sola transacción,
    // y la previsualización lo arma entero en memoria: sin tope, de 2026 a 9999 eran 2,9 millones de
    // funciones y el servidor se quedaba sin memoria.
    public static final int MAXIMO_DIAS = 366;

    private ValidadorProgramacion() {
    }

    // Sin hasta queda abierta: se extiende sola, de a catorce días. Los mensajes le dicen rango, como
    // el formulario.
    public static Periodo periodo(LocalDate desde, LocalDate hasta) {
        Regla.objeto(desde).obligatorio("Falta la fecha de inicio");
        Periodo periodo = Periodo.de(desde, hasta, "El rango");
        if (hasta != null && ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS) {
            throw new DatoInvalido("El rango no puede cubrir más de " + MAXIMO_DIAS + " días");
        }
        return periodo;
    }

    public static void hora(LocalTime hora) {
        Regla.objeto(hora).obligatorio("Falta la hora de la función");
        ValidadorFuncion.sinSegundos(hora, "La hora de la función");
    }

    // Sin días elegidos son todos. Con días, alguno tiene que caer en un rango cerrado: si no, la
    // grilla no generaría funciones. Uno abierto no tiene fin contra el cual medir: algún lunes llega.
    public static Set<DayOfWeek> dias(Set<DayOfWeek> dias, Periodo periodo) {
        Regla.lista(dias).sinNulos("Falta el día de la semana");
        Set<DayOfWeek> elegidos = dias == null || dias.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(dias);
        if (periodo.hasta() != null && periodo.desde().datesUntil(periodo.hasta().plusDays(1))
                .noneMatch(dia -> elegidos.isEmpty() || elegidos.contains(dia.getDayOfWeek()))) {
            throw new DatoInvalido(
                    "Ningún día del rango cae en los días elegidos: la grilla no generaría funciones");
        }
        return elegidos;
    }
}
