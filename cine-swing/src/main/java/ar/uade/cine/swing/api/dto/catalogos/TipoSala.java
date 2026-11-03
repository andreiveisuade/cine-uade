package ar.uade.cine.swing.api.dto.catalogos;

// Un tipo de sala del catálogo: su recargo, si proyecta 3D, si es premium y qué le toca en la grilla automática.
public record TipoSala(String nombre, double multiplicador, boolean soportaTresD, boolean premium,
                       String enLaGrilla) {
}
