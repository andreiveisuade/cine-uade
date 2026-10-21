package ar.uade.cine.model.tiempo;

import java.time.LocalTime;

import ar.uade.cine.model.rechazos.DatoInvalido;
import jakarta.persistence.Embeddable;

// Horas de desde a hasta en un mismo día, puntas incluidas; Value Object @Embeddable.
// Punta null = abierta; la que cruza medianoche o de un solo instante se rechaza en vez de guardarse muerta.
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
