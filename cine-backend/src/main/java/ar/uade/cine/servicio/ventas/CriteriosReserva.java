package ar.uade.cine.servicio.ventas;

import java.time.LocalDate;

import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.servicio.cartelera.DatosPelicula;

/**
 * Con qué se busca una reserva en el listado del panel.
 *
 * <p>Existe como tipo propio y no como tres parámetros sueltos por lo mismo que
 * {@link DatosPelicula}: son tres hoy y van a ser cinco, y una firma de cinco argumentos
 * del mismo tipo es una invitación a pasarlos en el orden equivocado sin que el
 * compilador diga nada.
 *
 * <p><strong>Vive en servicio y no en la capa HTTP.</strong> «Pendientes de cobro» o «las
 * de hoy» son preguntas del negocio, no de la pantalla: el día que aparezca otra pantalla
 * la misma lista, la pide igual. Es el mismo argumento por el que la bitácora se escribe
 * en el gestor — si el criterio viviera en la API, habría dos definiciones de lo mismo y
 * la nueva sería la que se olvida.
 *
 * @param estado solo las que están así, o {@code null} para todas
 * @param dia    solo las de funciones de esa fecha, o {@code null} para todas
 * @param texto  búsqueda libre sobre el nombre y el email del cliente, el título de la
 *               película, el código de la reserva y los códigos de butaca. Es lo que
 *               alguien tiene a mano en el mostrador cuando viene a preguntar
 */
public record CriteriosReserva(EstadoReserva estado, LocalDate dia, String texto) {

    /** Sin filtros: el listado completo. */
    public static CriteriosReserva ninguno() {
        return new CriteriosReserva(null, null, null);
    }

    /** El texto normalizado, o vacío si no hay. Buscar es sin distinguir mayúsculas. */
    public String textoNormalizado() {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    public boolean sinFiltros() {
        return estado == null && dia == null && textoNormalizado().isEmpty();
    }
}
