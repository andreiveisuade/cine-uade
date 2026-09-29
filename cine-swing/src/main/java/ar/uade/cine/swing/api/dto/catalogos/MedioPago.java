package ar.uade.cine.swing.api.dto.catalogos;

// Un medio de pago del catálogo; Swing pide el código de autorización por el flag, nunca por el nombre.
public record MedioPago(String nombre, boolean requiereAutorizacion) {
}
