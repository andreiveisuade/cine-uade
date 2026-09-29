package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.dinero.Dinero;

// Pedido de grilla automática del encargado; Value Object: completa los defaults y se valida al nacer.
// Se valida al construirse: el planificador nunca ve unos criterios imposibles, y se queda solo
// con lo que necesita la base (que haya salas).
public record CriteriosGrilla(LocalDate desde, int dias, LocalTime apertura, LocalTime cierre,
                              int cuantasPeliculas, Dinero precio, Version version,
                              Proyeccion proyeccion) {

    // La propuesta se arma entera en memoria, pase por pase: sin tope, un pedido de años la tumba.
    public static final int MAXIMO_DIAS = 31;

    public CriteriosGrilla {
        if (desde == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio de la grilla");
        }
        if (dias <= 0) {
            throw new IllegalArgumentException("La grilla tiene que cubrir al menos un día");
        }
        if (dias > MAXIMO_DIAS) {
            throw new IllegalArgumentException("La grilla no puede cubrir más de " + MAXIMO_DIAS + " días");
        }
        if (cuantasPeliculas <= 0) {
            throw new IllegalArgumentException("Hay que programar al menos una película");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (!apertura.isBefore(efectivo(cierre))) {
            throw new IllegalArgumentException("El cine tiene que cerrar después de abrir");
        }
    }

    public static CriteriosGrilla deUnaSemana(LocalDate desde, int cuantasPeliculas, Dinero precio) {
        return completando(desde, null, null, null, cuantasPeliculas, precio, null, null);
    }

    // Lo que el pedido no trae es una semana de 14 a 0 con 8 películas subtituladas en 2D. Se
    // completa y se valida en una sola construcción: el primer error es el del pedido, no el de un default.
    public static CriteriosGrilla completando(LocalDate desde, Integer dias, LocalTime apertura,
                                              LocalTime cierre, Integer cuantasPeliculas, Dinero precio,
                                              Version version, Proyeccion proyeccion) {
        return new CriteriosGrilla(desde,
                dias == null ? 7 : dias,
                apertura == null ? LocalTime.of(14, 0) : apertura,
                cierre == null ? LocalTime.MIDNIGHT : cierre,
                cuantasPeliculas == null ? 8 : cuantasPeliculas,
                precio,
                version == null ? Version.SUBTITULADA : version,
                proyeccion == null ? Proyeccion.DOS_D : proyeccion);
    }

    public LocalTime cierreEfectivo() {
        return efectivo(cierre);
    }

    // Un cierre 00:00 es el final del día, no su principio.
    private static LocalTime efectivo(LocalTime cierre) {
        return cierre.equals(LocalTime.MIDNIGHT) ? LocalTime.of(23, 59) : cierre;
    }
}
