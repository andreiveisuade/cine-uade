package ar.uade.cine.service.ventas;

import java.time.LocalDate;

import ar.uade.cine.model.ventas.EstadoReserva;

// Filtros del listado de reservas (estado, día, texto); record que normaliza el texto y sabe si está vacío.
public record CriteriosReserva(EstadoReserva estado, LocalDate dia, String texto) {

    public String textoNormalizado() {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    public boolean sinFiltros() {
        return estado == null && dia == null && textoNormalizado().isEmpty();
    }
}
