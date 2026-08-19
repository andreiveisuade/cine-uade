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
}
