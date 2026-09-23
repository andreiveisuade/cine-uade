package ar.uade.cine.model.ventas;

/**
 * Con qué se cobró. Los medios electrónicos devuelven un código de autorización del
 * procesador; el efectivo no, y por eso ese campo admite vacío.
 */
public enum MedioPago {

    EFECTIVO(false),
    DEBITO(true),
    CREDITO(true),
    QR(true),
    TRANSFERENCIA(true);

    private final boolean requiereAutorizacion;

    MedioPago(boolean requiereAutorizacion) {
        this.requiereAutorizacion = requiereAutorizacion;
    }

    public boolean requiereAutorizacion() {
        return requiereAutorizacion;
    }

    /**
     * R11: el código de autorización, limpio, o el error si el medio lo exige y no vino.
     * Vive acá para que boletería y candy lo apliquen igual.
     */
    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (requiereAutorizacion && limpio.isEmpty()) {
            throw new IllegalArgumentException("El pago con " + this + " necesita código de autorización");
        }
        return limpio;
    }
}
