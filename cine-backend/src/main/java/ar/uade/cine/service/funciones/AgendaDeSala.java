package ar.uade.cine.service.funciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.model.funciones.Funcion;

// Única definición de R3: se pisan si cada una empieza antes de que termine la otra, limpieza incluida.
public final class AgendaDeSala {

    public record Tramo(Funcion funcion, LocalDateTime inicio, LocalDateTime fin) {
    }

    private final int minutosLimpieza;
    private final List<Tramo> tomados;

    AgendaDeSala(int minutosLimpieza, List<Tramo> tomados) {
        this.minutosLimpieza = minutosLimpieza;
        this.tomados = tomados;
    }

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
