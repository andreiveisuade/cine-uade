package ar.uade.cine.dto.ventas;

import java.util.List;

public record BloqueoVistaDTO(String sesion, List<String> butacas, List<String> rechazadas,
                              long vencenEnSegundos) {
}
