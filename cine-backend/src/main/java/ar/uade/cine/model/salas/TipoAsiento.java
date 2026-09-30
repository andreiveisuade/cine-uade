package ar.uade.cine.model.salas;

import lombok.Getter;

import ar.uade.cine.model.dinero.Dinero;

// Tipo de butaca; cada constante trae su multiplicador de precio, así el cálculo no necesita un switch.
@Getter
public enum TipoAsiento {

    ESTANDAR(1.0),
    VIP(1.5),
    PAREJA(1.8),
    ACCESIBLE(1.0);

    private final double multiplicadorPrecio;

    TipoAsiento(double multiplicadorPrecio) {
        this.multiplicadorPrecio = multiplicadorPrecio;
    }

    // Su parte del precio de una entrada: el recargo de la butaca sobre el precio en la sala.
    public Dinero aplicarA(Dinero precio) {
        return precio.por(multiplicadorPrecio);
    }
}
