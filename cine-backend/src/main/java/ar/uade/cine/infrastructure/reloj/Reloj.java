package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * De dónde sale "ahora". Los gestores lo reciben en vez de llamar a {@code now()} para que
 * las reglas que dependen del tiempo (R17, R19, la espera entre importaciones) se puedan
 * probar con un reloj que se mueve a mano.
 */
@FunctionalInterface
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
