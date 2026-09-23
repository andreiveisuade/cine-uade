package ar.uade.cine.service.funciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.model.funciones.Funcion;

/**
 * Lo que una sala ya tiene tomado, y la única definición de R3: dos funciones se pisan
 * si cada una empieza antes de que termine la otra, contando la limpieza.
 *
 * <p>Lo que ocupa una función es su duración <strong>más la limpieza</strong>: programar
 * a las 22:05 algo que termina 22:00 es empezar con la gente adentro barriendo. El margen
 * se suma a los dos lados porque la función nueva también deja la sala sucia.
 *
 * <p>Existe como objeto aparte por costo: {@code GestorFunciones} la arma con una lectura
 * y el planificador le pregunta cientos de veces sin volver a la base.
 */
public final class AgendaDeSala {

    /** Un rato en que la sala está tomada: la función más su limpieza. */
    public record Tramo(Funcion funcion, LocalDateTime inicio, LocalDateTime fin) {
    }

    private final int minutosLimpieza;
    private final List<Tramo> tomados;

    AgendaDeSala(int minutosLimpieza, List<Tramo> tomados) {
        this.minutosLimpieza = minutosLimpieza;
        this.tomados = tomados;
    }

    /** La función que se pisa con ese rango, si hay alguna. */
    public Optional<Funcion> chocaCon(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime finConLimpieza = fin.plusMinutes(minutosLimpieza);
        return tomados.stream()
                .filter(t -> inicio.isBefore(t.fin()) && t.inicio().isBefore(finConLimpieza))
                .map(Tramo::funcion)
                .findFirst();
    }

    public boolean chocaEn(LocalDateTime inicio, LocalDateTime fin) {
        return chocaCon(inicio, fin).isPresent();
    }
}
