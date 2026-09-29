package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;

// La hora actual como puerto; los tests la mueven a mano con RelojMovible (Variaciones protegidas).
@FunctionalInterface
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
