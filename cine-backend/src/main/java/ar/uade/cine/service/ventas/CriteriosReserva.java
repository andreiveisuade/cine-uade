package ar.uade.cine.service.ventas;

import java.time.LocalDate;

import ar.uade.cine.model.ventas.EstadoReserva;

public record CriteriosReserva(EstadoReserva estado, LocalDate dia, String texto) {

    public String textoNormalizado() {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    public boolean sinFiltros() {
        return estado == null && dia == null && textoNormalizado().isEmpty();
    }
}
