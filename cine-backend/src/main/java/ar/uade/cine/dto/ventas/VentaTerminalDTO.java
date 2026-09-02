package ar.uade.cine.dto.ventas;

import java.util.Map;

import ar.uade.cine.model.ventas.TipoTarifa;

public record VentaTerminalDTO(
        Integer idLocal,
        Integer funcionId,
        String nombreCliente,
        String emailCliente,
        Map<String, TipoTarifa> butacas,
        String medioPago,
        String codigoAutorizacion
) {}