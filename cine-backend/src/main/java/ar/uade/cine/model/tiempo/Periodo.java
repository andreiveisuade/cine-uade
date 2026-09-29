package ar.uade.cine.model.tiempo;

import java.time.LocalDate;

import ar.uade.cine.model.rechazos.DatoInvalido;
import jakarta.persistence.Embeddable;

// Días de desde a hasta, las dos puntas incluidas; Value Object @Embeddable que no se arma al revés.
// Una punta en null queda abierta: la grilla sin fin es un período sin hasta. Si una punta es
// obligatoria lo decide la entidad, con su propio «Falta…», antes de armarlo.
// Record: Hibernate 6 arma un @Embeddable record con su constructor canónico, y la entidad lo mapea
// sobre sus columnas con @AttributeOverride. Con las dos columnas en null, el campo queda en null.
@Embeddable
public record Periodo(LocalDate desde, LocalDate hasta) {

    // El canónico lo llama «El período»: lo usan Hibernate al leer y quien no necesita otro nombre.
    public Periodo {
        exigirOrden(desde, hasta, "El período");
    }

    // sujeto es qué es, con artículo y mayúscula («La vigencia», «El rango»): arranca el mensaje.
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
