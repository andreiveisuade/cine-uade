package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;

@FunctionalInterface
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
