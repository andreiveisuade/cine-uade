package ar.uade.cine.swing.api.dto.catalogos;

// Una tarifa de entrada del catálogo y si hay que acreditarla con carnet.
public record Tarifa(String nombre, boolean requiereAcreditacion) {
}
