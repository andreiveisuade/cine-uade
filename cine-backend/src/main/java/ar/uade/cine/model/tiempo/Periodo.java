package ar.uade.cine.model.tiempo;

import java.time.LocalDate;

import ar.uade.cine.model.rechazos.DatoInvalido;
import jakarta.persistence.Embeddable;

// Días de desde a hasta, puntas incluidas; Value Object @Embeddable que no se arma al revés.
// Punta null = abierta; si es obligatoria lo decide la entidad antes de armarlo.
@Embeddable
public record Periodo(LocalDate desde, LocalDate hasta) {

    public Periodo {
        exigirOrden(desde, hasta, "El período");
    }

    // sujeto es qué es, con artículo y mayúscula («La vigencia»): arranca el mensaje.
    public static Periodo de(LocalDate desde, LocalDate hasta, String sujeto) {
        exigirOrden(desde, hasta, sujeto);
        return new Periodo(desde, hasta);
    }

    public boolean incluye(LocalDate dia) {
        boolean empezo = desde == null || !dia.isBefore(desde);
        boolean noTermino = hasta == null || !dia.isAfter(hasta);
        return empezo && noTermino;
    }

    private static void exigirOrden(LocalDate desde, LocalDate hasta, String sujeto) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new DatoInvalido(sujeto + " tiene que empezar antes de terminar");
        }
    }
}
