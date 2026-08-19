package ar.uade.cine.model.ventas;

/**
 * Quién compra la entrada. Es el tercer eje del precio: los otros dos son la tecnología
 * de la sala y el tipo de butaca, que dicen <em>dónde</em> se ve la película; este dice
 * <em>quién</em> la ve.
 *
 * <p>Va en la {@link Entrada} y no en la reserva porque es por persona: en una reserva de
 * cuatro butacas puede haber dos generales, un menor y un jubilado.
 *
 * <p>Como el cliente no inicia sesión, el sistema no tiene fecha de nacimiento ni forma de
 * comprobar que alguien es jubilado: la tarifa se <strong>declara</strong> al comprar y se
 * <strong>acredita en la puerta</strong>, igual que en un cine real. Por eso cada tarifa
 * sabe si hay que pedir un carnet, y quien valida la entrada lo ve en pantalla.
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

    /** Si en la puerta hay que pedir un carnet que respalde lo que se declaró al comprar. */
    public boolean requiereAcreditacion() {
        return requiereAcreditacion;
    }
}
