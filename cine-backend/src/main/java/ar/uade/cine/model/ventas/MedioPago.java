package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Medio de pago (R11) y su etiqueta; Experto: qué autorización lleva cada uno y por dónde se cobra.
// La etiqueta es para los mensajes ("el pago con crédito"); en el JSON y en la base va name().
@Getter
@Accessors(fluent = true)
public enum MedioPago {

    EFECTIVO("efectivo", false),
    DEBITO("débito", true),
    CREDITO("crédito", true),
    QR("QR", true),
    TRANSFERENCIA("transferencia", true);

    // El VARCHAR(50) de pago y compra_candy: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_AUTORIZACION = 50;

    private final String etiqueta;
    private final boolean requiereAutorizacion;

    MedioPago(String etiqueta, boolean requiereAutorizacion) {
        this.etiqueta = etiqueta;
        this.requiereAutorizacion = requiereAutorizacion;
    }

    // El código como se guarda: los electrónicos lo exigen y el efectivo no lleva. Un código en un cobro en
    // efectivo es un error de carga, y el comprobante mostraría una autorización que nadie dio.
    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.strip();
        if (requiereAutorizacion) {
            Regla.texto(limpio).obligatorio("Falta el código de autorización del pago con " + etiqueta);
        } else if (!limpio.isEmpty()) {
            throw new DatoInvalido("El pago en " + etiqueta + " no lleva código de autorización");
        }
        return Regla.texto(limpio).hasta(LARGO_AUTORIZACION, "El código de autorización").valor();
    }

    // El efectivo se cobra en la caja del cine: no pasa por la pasarela y lleva recibo de caja.
    public boolean seCobraEnCaja() {
        return !requiereAutorizacion;
    }

    // Mandar el efectivo a la pasarela abriría un cobro que nadie puede autorizar.
    public void exigirCheckout() {
        if (seCobraEnCaja()) {
            throw new DatoInvalido("El pago con " + etiqueta + " no va por checkout: se cobra en la caja del cine");
        }
    }
}
