package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Entrada;

public class EntradaImpl implements Entrada {

    private final int asientoId;
    private final String codigoAsiento;
    private final double precio;

    public EntradaImpl(int asientoId, String codigoAsiento, double precio) {
        this.asientoId = asientoId;
        this.codigoAsiento = codigoAsiento;
        this.precio = precio;
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
    public double getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return codigoAsiento;
    }
}
