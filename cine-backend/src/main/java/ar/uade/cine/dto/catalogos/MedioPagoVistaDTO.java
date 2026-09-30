package ar.uade.cine.dto.catalogos;

// Un medio de pago de GET /api/medios-pago; los clientes deciden por requiereAutorizacion, no por el nombre.
public record MedioPagoVistaDTO(String nombre, boolean requiereAutorizacion) {
}
