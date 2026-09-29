package ar.uade.cine.service.informes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Qué días cubre una declaración jurada; Value Object que se valida al construirse.
// Separado de la acumulación (DeclaracionJurada#de) para que las reglas del período —el default, que
// desde no pase a hasta, el tope— estén en un solo lugar.
public record PeriodoDeclarado(LocalDate desde, LocalDate hasta) {

    // El archivo se arma entero en memoria: el tope lo acota, y un mes cubre cualquier cierre del INCAA.
    public static final int MAXIMO_DIAS = 31;

    public PeriodoDeclarado {
        if (desde == null || hasta == null) {
            throw new DatoInvalido(
                    "Hay que indicar desde y hasta, o ninguna de las dos para la última semana cinematográfica");
        }
        if (desde.isAfter(hasta)) {
            throw new DatoInvalido("El período tiene que empezar antes de terminar");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS) {
            throw new DatoInvalido("El período no puede superar los " + MAXIMO_DIAS + " días");
        }
    }

    // Sin fechas, la última semana cinematográfica completa: de jueves a miércoles, la que ya cerró.
    public static PeriodoDeclarado de(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        if (desde == null && hasta == null) {
            LocalDate juevesDeEstaSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
            return new PeriodoDeclarado(juevesDeEstaSemana.minusWeeks(1), juevesDeEstaSemana.minusDays(1));
        }
        return new PeriodoDeclarado(desde, hasta);
    }

    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    public LocalDateTime fin() {
        return hasta.plusDays(1).atStartOfDay();
    }
}
