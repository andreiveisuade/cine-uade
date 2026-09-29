package ar.uade.cine.dto.catalogos;

import java.util.List;

// Un tipo de promoción de GET /api/tipos-promocion, con los campos de beneficio que pide su alta.
public record TipoPromocionVistaDTO(String nombre, List<String> campos) {
}
