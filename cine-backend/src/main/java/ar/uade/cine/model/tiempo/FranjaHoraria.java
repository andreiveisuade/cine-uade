package ar.uade.cine.model.tiempo;

import java.time.LocalTime;

import ar.uade.cine.model.rechazos.DatoInvalido;
import jakarta.persistence.Embeddable;

// Horas de desde a hasta dentro de un mismo día, puntas incluidas; Value Object @Embeddable.
// Las dos puntas son opcionales, como en las promociones: la que falta queda abierta. Una franja que
// cruza la medianoche (22 a 2) no incluiría ninguna hora, y una de un solo instante no es franja:
// las dos se rechazan en vez de guardarse muertas.
// Record: Hibernate 6 arma un @Embeddable record con su constructor canónico. Con las dos columnas en
// null deja el campo en null, así que la entidad lo lee como todo el día.
@Embeddable
public record FranjaHoraria(LocalTime desde, LocalTime hasta) {

    public FranjaHoraria {
        if (desde != null && hasta != null && !hasta.isAfter(desde)) {
            throw new DatoInvalido("La franja horaria tiene que empezar antes de terminar");
        }
    }

    public boolean incluye(LocalTime hora) {
        boolean empezo = desde == null || !hora.isBefore(desde);
        boolean noTermino = hasta == null || !hora.isAfter(hasta);
        return empezo && noTermino;
    }
}
