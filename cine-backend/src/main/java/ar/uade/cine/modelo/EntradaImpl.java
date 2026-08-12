package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Entrada;

public class EntradaImpl implements Entrada {

    private final int asientoId;
    private final String codigoAsiento;

    public EntradaImpl(int asientoId, String codigoAsiento) {
        this.asientoId = asientoId;
        this.codigoAsiento = codigoAsiento;
    }

    @Override
    public int getAsientoId() {
        return asientoId;
    }

    @Override
    public String getCodigoAsiento() {
        return codigoAsiento;
    }

    @Override
    public String toString() {
        return codigoAsiento;
    }
}
