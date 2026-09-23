package ar.uade.cine.service;

// Un duplicado: el pedido es válido, pero choca con algo que ya existe (mismo nombre, email o título).
public class ConflictoDeNegocio extends IllegalArgumentException {

    public ConflictoDeNegocio(String mensaje) {
        super(mensaje);
    }
}
