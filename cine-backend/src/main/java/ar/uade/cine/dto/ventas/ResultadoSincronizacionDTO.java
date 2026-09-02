package ar.uade.cine.dto.ventas;

public record ResultadoSincronizacionDTO(
        Integer idLocal,
        boolean aceptada,
        String motivoRechazo
) {}