package ar.uade.cine.model.ventas;

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

    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (requiereAutorizacion && limpio.isEmpty()) {
            throw new IllegalArgumentException("El pago con " + this + " necesita código de autorización");
        }
        return limpio;
    }
}
