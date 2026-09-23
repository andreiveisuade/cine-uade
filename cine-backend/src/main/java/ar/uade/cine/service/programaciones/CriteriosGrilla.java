package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.dinero.Dinero;

public record CriteriosGrilla(LocalDate desde, int dias, LocalTime apertura, LocalTime cierre,
                              int cuantasPeliculas, Dinero precio, Version version,
                              Proyeccion proyeccion) {

    public static CriteriosGrilla deUnaSemana(LocalDate desde, int cuantasPeliculas, Dinero precio) {
        return new CriteriosGrilla(desde, 7, LocalTime.of(14, 0), LocalTime.of(0, 0),
                cuantasPeliculas, precio, Version.SUBTITULADA, Proyeccion.DOS_D);
    }

    // Un cierre 00:00 es el final del día, no su principio.
    public LocalTime cierreEfectivo() {
        return cierre.equals(LocalTime.MIDNIGHT) ? LocalTime.of(23, 59) : cierre;
    }
}
