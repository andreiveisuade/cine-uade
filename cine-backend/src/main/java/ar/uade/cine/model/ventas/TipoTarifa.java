package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.dinero.Dinero;

// Tarifa de una entrada; cada constante trae su multiplicador, si pide acreditación y si entra en promos.
@Getter
public enum TipoTarifa {

    GENERAL(1.0, false, true),
    MENOR(0.6, true, false),
    JUBILADO(0.5, true, false),
    ESTUDIANTE(0.7, true, false);

    private final double multiplicadorPrecio;
    @Accessors(fluent = true)
    private final boolean requiereAcreditacion;
    // R16: una tarifa reducida ya es un descuento y no se le suma una promoción. Una tarifa nueva
    // dice acá si participa, sin tocar a GestorPromociones.
    @Accessors(fluent = true)
    private final boolean participaDePromociones;

    TipoTarifa(double multiplicadorPrecio, boolean requiereAcreditacion, boolean participaDePromociones) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.requiereAcreditacion = requiereAcreditacion;
        this.participaDePromociones = participaDePromociones;
    }

    // La última parte del precio de una entrada: la reducción de la tarifa sobre el precio de la butaca.
    public Dinero aplicarA(Dinero precio) {
        return precio.por(multiplicadorPrecio);
    }
}
