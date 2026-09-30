package ar.uade.cine.swing.api.dto.cartelera;

// Si el importador de TMDB puede correr; detalle es el aviso que muestra la pantalla cuando falta el token.
public record EstadoImportador(boolean disponible, String detalle) {
}
