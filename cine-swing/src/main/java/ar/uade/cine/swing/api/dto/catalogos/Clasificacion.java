package ar.uade.cine.swing.api.dto.catalogos;

// Una clasificación por edad del catálogo, con la edad mínima que exige; llena el combo de Películas.
public record Clasificacion(String nombre, int edadMinima) {
}
