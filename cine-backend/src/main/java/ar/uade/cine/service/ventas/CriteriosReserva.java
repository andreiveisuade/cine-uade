package ar.uade.cine.service.ventas;

import java.time.LocalDate;

import ar.uade.cine.model.ventas.EstadoReserva;

/**
 * Filtros del listado de reservas. Vive en service porque «pendientes de cobro» es una
 * pregunta del negocio, no de la pantalla.
 *
 * @param texto búsqueda libre sobre cliente, película, código de reserva y butacas
 */
public record CriteriosReserva(EstadoReserva estado, LocalDate dia, String texto) {

    public static CriteriosReserva ninguno() {
        return new CriteriosReserva(null, null, null);
    }

    /** Vacío si no hay: se busca sin distinguir mayúsculas. */
    public String textoNormalizado() {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    public boolean sinFiltros() {
        return estado == null && dia == null && textoNormalizado().isEmpty();
    }
}
