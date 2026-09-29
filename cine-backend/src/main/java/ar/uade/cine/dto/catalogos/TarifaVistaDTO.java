package ar.uade.cine.dto.catalogos;

// Una tarifa de entrada de GET /api/tarifas, con su multiplicador y si hay que acreditarla con carnet.
public record TarifaVistaDTO(String nombre, double multiplicador, boolean requiereAcreditacion) {
}
