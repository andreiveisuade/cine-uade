package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Puerto: la hora actual; los tests la mueven a mano con RelojMovible (Variaciones protegidas).
@FunctionalInterface
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
