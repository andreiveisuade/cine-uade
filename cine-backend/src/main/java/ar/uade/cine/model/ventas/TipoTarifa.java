package ar.uade.cine.model.ventas;

public enum TipoTarifa {

    GENERAL(1.0, false),
    MENOR(0.6, true),
    JUBILADO(0.5, true),
    ESTUDIANTE(0.7, true);

    private final double multiplicadorPrecio;
    private final boolean requiereAcreditacion;

    TipoTarifa(double multiplicadorPrecio, boolean requiereAcreditacion) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.requiereAcreditacion = requiereAcreditacion;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }

    public boolean requiereAcreditacion() {
        return requiereAcreditacion;
    }
}
