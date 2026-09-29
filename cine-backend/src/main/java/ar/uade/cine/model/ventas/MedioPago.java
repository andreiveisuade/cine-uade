package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

// Medio de pago (R11); Experto: exige código de autorización a los electrónicos y valida su largo.
@Getter
@Accessors(fluent = true)
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

    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (requiereAutorizacion && limpio.isEmpty()) {
            throw new IllegalArgumentException("El pago con " + this + " necesita código de autorización");
        }
        // El VARCHAR(50) de pago y compra_candy: pasado, MySQL rechaza el INSERT con un 500.
        if (limpio.length() > 50) {
            throw new IllegalArgumentException("El código de autorización no puede tener más de 50 caracteres");
        }
        return limpio;
    }
}
