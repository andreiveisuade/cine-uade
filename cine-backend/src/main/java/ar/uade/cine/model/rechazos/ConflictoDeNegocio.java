package ar.uade.cine.model.rechazos;

// Un duplicado: pedido válido que choca con algo que ya existe (nombre, email o título); se responde 409.
public final class ConflictoDeNegocio extends Rechazo {

    public ConflictoDeNegocio(String mensaje) {
        super(mensaje);
    }
}
