package ar.uade.cine.dto.catalogos;

import java.util.List;

// Un tipo de sala de GET /api/tipos-sala: multiplicador, qué proyecta (R8, R35), si es premium y su papel en la grilla.
public record TipoSalaVistaDTO(String nombre, double multiplicador, List<String> proyecciones, boolean premium,
                               String enLaGrilla) {
}
