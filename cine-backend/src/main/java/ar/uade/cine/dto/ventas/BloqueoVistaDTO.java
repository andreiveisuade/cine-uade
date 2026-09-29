package ar.uade.cine.dto.ventas;

import java.util.List;

// Lo que sale al bloquear butacas (POST /api/funciones/{id}/bloqueos); perder una va en rechazadas, no 409.
public record BloqueoVistaDTO(String sesion, List<String> butacas, List<String> rechazadas,
                              long vencenEnSegundos) {
}
