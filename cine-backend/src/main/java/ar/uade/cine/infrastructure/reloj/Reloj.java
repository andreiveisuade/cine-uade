package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Puerto: la hora actual; los tests la mueven a mano con RelojMovible (Variaciones protegidas).
@FunctionalInterface
public interface Reloj {

    // Fija: el contenedor de deploy corre en UTC y las reglas horarias son las del cine.
    ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
