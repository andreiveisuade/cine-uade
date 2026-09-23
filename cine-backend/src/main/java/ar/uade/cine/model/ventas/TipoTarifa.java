package ar.uade.cine.model.ventas;

/**
 * Quién ve la película: el tercer eje del precio, junto con sala y butaca. Como el cliente
 * no inicia sesión, la tarifa se declara al comprar y se acredita con carnet en la puerta.
 */
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
