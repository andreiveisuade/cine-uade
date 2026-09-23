package ar.uade.cine.dto.ventas;

import java.util.List;

/**
 * {@code rechazadas} viaja aparte y no como error: perder una butaca mientras se elige no
 * invalida las demás. {@code vencenEnSegundos} deja la duración del bloqueo en el backend.
 */
public record BloqueoVistaDTO(String sesion, List<String> butacas, List<String> rechazadas,
                              long vencenEnSegundos) {
}
