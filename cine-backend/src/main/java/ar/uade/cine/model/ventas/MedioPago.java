package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Medio de pago (R11) y su etiqueta; Experto: qué autorización lleva y por dónde se cobra cada uno.
public enum MedioPago {

    EFECTIVO("efectivo", false),
    DEBITO("débito", true),
    CREDITO("crédito", true),
    QR("QR", true),
    TRANSFERENCIA("transferencia", true),
    // La web cobra solo con este: el comprador elige tarjeta o saldo adentro de Mercado Pago.
    MERCADO_PAGO("Mercado Pago", true);

    // VARCHAR(50) de pago y compra_candy: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_AUTORIZACION = 50;

    private final String etiqueta;
    @Getter
    @Accessors(fluent = true)
    private final boolean requiereAutorizacion;

    MedioPago(String etiqueta, boolean requiereAutorizacion) {
        this.etiqueta = etiqueta;
        this.requiereAutorizacion = requiereAutorizacion;
    }

    // Los electrónicos exigen código y el efectivo no lleva: si no, el comprobante mostraría una autorización que nadie dio.
    public String autorizacion(String codigo) {
        String limpio = codigo == null ? "" : codigo.strip();
        if (requiereAutorizacion) {
            Regla.texto(limpio).obligatorio("Falta el código de autorización del pago con " + etiqueta);
        } else if (!limpio.isEmpty()) {
            throw new DatoInvalido("El pago en " + etiqueta + " no lleva código de autorización");
        }
        return Regla.texto(limpio).hasta(LARGO_AUTORIZACION, "El código de autorización").valor();
    }

    public boolean seCobraEnCaja() {
        return !requiereAutorizacion;
    }

    public boolean seCobraEnLaWeb() {
        return this == MERCADO_PAGO;
    }

    public void exigirCheckout() {
        if (seCobraEnCaja()) {
            throw new DatoInvalido("El pago con " + etiqueta + " no va por checkout: se cobra en la caja del cine");
        }
    }
}
