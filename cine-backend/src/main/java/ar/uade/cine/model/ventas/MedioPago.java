package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

// Medio de pago (R11) y su etiqueta; Experto: exige autorización a los electrónicos y valida su largo.
// La etiqueta es para los mensajes ("el pago con crédito"); en el JSON y en la base va name().
@Getter
@Accessors(fluent = true)
public enum MedioPago {

    EFECTIVO("efectivo", false),
    DEBITO("débito", true),
    CREDITO("crédito", true),
    QR("QR", true),
    TRANSFERENCIA("transferencia", true);

    private final String etiqueta;
    private final boolean requiereAutorizacion;

    MedioPago(String etiqueta, boolean requiereAutorizacion) {
        this.etiqueta = etiqueta;
        this.requiereAutorizacion = requiereAutorizacion;
    }

    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (requiereAutorizacion && limpio.isEmpty()) {
            throw new IllegalArgumentException("Falta el código de autorización del pago con " + etiqueta);
        }
        // El VARCHAR(50) de pago y compra_candy: pasado, MySQL rechaza el INSERT con un 500.
        if (limpio.length() > 50) {
            throw new IllegalArgumentException("El código de autorización no puede tener más de 50 caracteres");
        }
        return limpio;
    }
}
