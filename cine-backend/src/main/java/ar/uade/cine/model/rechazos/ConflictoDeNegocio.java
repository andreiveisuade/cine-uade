package ar.uade.cine.model.rechazos;

// Pedido válido que choca con el estado: un duplicado o un recurso en uso; se responde 409.
public final class ConflictoDeNegocio extends Rechazo {

    public ConflictoDeNegocio(String mensaje) {
        super(mensaje);
    }
}
