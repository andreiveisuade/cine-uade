package ar.uade.cine.service;

// Un duplicado: pedido válido que choca con algo que ya existe (nombre, email o título); se responde 409.
public class ConflictoDeNegocio extends IllegalArgumentException {

    public ConflictoDeNegocio(String mensaje) {
        super(mensaje);
    }
}
