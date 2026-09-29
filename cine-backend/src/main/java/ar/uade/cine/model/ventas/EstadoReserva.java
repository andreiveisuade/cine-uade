package ar.uade.cine.model.ventas;

import lombok.Getter;
import lombok.experimental.Accessors;

// Estados de una reserva y su etiqueta; solo RESERVADA cambia: se cobra, cancela o expira (R5, R13, R17).
// La etiqueta es para los mensajes ("La reserva está vencida"); en el JSON y en la base va name().
@Getter
@Accessors(fluent = true)
public enum EstadoReserva {

    RESERVADA("sin pagar"),
    PAGADA("pagada"),
    CANCELADA("cancelada"),
    EXPIRADA("vencida");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
