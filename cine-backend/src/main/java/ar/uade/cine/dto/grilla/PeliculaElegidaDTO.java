package ar.uade.cine.dto.grilla;

import java.util.List;

/**
 * Una película del elenco, con lo que explica por qué entró: su puntaje y sus géneros.
 *
 * <p>No es la {@code PeliculaVistaDTO} completa a propósito: acá no se está mostrando la
 * película sino defendiendo una decisión, y la sinopsis o el poster no defienden nada.
 */
public record PeliculaElegidaDTO(int id, String titulo, double puntaje, int duracionMinutos,
                                 List<String> generos, int pases) {
}
