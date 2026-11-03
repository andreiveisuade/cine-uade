package ar.uade.cine.model.salas;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.dinero.Dinero;

// Formato de sala; Experto: trae su multiplicador de precio, si proyecta 3D (R8) y si es premium.
@Getter
public enum TipoSala {

    DOS_D(1.0, false, false),
    TRES_D(1.3, true, false),
    IMAX(1.6, true, true),
    CUATRO_D(1.8, true, true);

    private final double multiplicadorPrecio;
    @Accessors(fluent = true)
    private final boolean soportaTresD;
    // La grilla le da las mejor puntuadas: es la sala que más cobra por la misma película.
    @Accessors(fluent = true)
    private final boolean premium;

    TipoSala(double multiplicadorPrecio, boolean soportaTresD, boolean premium) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.soportaTresD = soportaTresD;
        this.premium = premium;
    }

    // Su parte del precio de una entrada: el recargo del formato sobre el precio base de la función.
    public Dinero aplicarA(Dinero precio) {
        return precio.por(multiplicadorPrecio);
    }
}
