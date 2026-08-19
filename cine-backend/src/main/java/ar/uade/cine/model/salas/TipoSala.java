package ar.uade.cine.model.salas;

/**
 * Tecnología de proyección que tiene instalada la sala. El multiplicador es sobre el
 * precio base de la función: una butaca de IMAX cuesta más que una de 2D aunque den la
 * misma película.
 *
 * <p>Solo describe la tecnología, no la categoría comercial: lo premium de una butaca
 * lo dice {@link TipoAsiento}. Si estuviera en los dos lados el recargo se cobraría
 * dos veces.
 */
public enum TipoSala {

    DOS_D(1.0, false),
    TRES_D(1.3, true),
    IMAX(1.6, true),
    CUATRO_D(1.8, true);

    private final double multiplicadorPrecio;
    private final boolean soportaTresD;

    TipoSala(double multiplicadorPrecio, boolean soportaTresD) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.soportaTresD = soportaTresD;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }

    /** Una sala 2D no tiene el proyector para dar funciones en 3D. */
    public boolean soportaTresD() {
        return soportaTresD;
    }
}
