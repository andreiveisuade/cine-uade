package ar.uade.cine.swing.api.dto.catalogos;

import java.util.List;

// Un tipo de sala del catálogo: su recargo, qué proyecta, si es premium y qué le toca en la grilla automática.
public record TipoSala(String nombre, double multiplicador, List<String> proyecciones, boolean premium,
                       String enLaGrilla) {
}
