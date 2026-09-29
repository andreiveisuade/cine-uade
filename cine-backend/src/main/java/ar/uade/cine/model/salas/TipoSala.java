package ar.uade.cine.model.salas;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.dinero.Dinero;

// Formato de sala; cada constante trae su multiplicador de precio y si proyecta 3D (R8).
@Getter
public enum TipoSala {

    DOS_D(1.0, false),
    TRES_D(1.3, true),
    IMAX(1.6, true),
    CUATRO_D(1.8, true);

    private final double multiplicadorPrecio;
    @Accessors(fluent = true)
    private final boolean soportaTresD;

    TipoSala(double multiplicadorPrecio, boolean soportaTresD) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.soportaTresD = soportaTresD;
    }

    // Su parte del precio de una entrada: el recargo del formato sobre el precio base de la función.
    public Dinero aplicarA(Dinero precio) {
        return precio.por(multiplicadorPrecio);
    }
}
