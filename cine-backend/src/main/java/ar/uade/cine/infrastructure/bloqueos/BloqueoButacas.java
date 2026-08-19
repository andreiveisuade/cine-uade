package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.util.Map;

/**
 * Quién tiene tomada una butaca <em>mientras la está eligiendo</em>, antes de que exista
 * una reserva.
 *
 * <p>Es la etapa que al sistema le faltaba. Una reserva RESERVADA ya retiene sus butacas
 * treinta minutos, pero recién existe cuando el cliente confirmó con su nombre y su mail:
 * hasta ese momento, dos personas eligiendo la misma butaca ven las dos que está libre, y
 * la segunda se entera de que la perdió recién al confirmar. Esto tapa esa ventana.
 *
 * <p>No es un DAO aunque viva en este paquete: no guarda datos del negocio, no se lista ni
 * se audita, y lo que escribe <strong>tiene que borrarse solo</strong>. Por eso toda
 * operación lleva un vencimiento en vez de un borrado explícito: quien cierra la pestaña
 * del navegador no avisa, y una butaca bloqueada para siempre es peor que una butaca sin
 * bloquear.
 *
 * <p>La duración entra por parámetro y no es una constante de acá: cuánto tiempo se le
 * guarda una butaca a alguien es una regla del negocio y vive en
 * {@link ar.uade.cine.service.ventas.Ocupacion}. Esta interfaz solo sabe tomar, soltar y contar.
 */
public interface BloqueoButacas {

    /**
     * Toma la butaca para esa sesión, o le renueva el vencimiento si ya era suya.
     *
     * <p>Renovar y tomar son la misma operación a propósito: el que elige vuelve a pedir
     * las mismas butacas cada vez que toca el mapa, y si eso no renovara el vencimiento,
     * las perdería a mitad de la compra por haber tardado en decidir.
     *
     * @return {@code false} si la tiene otra sesión
     */
    boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion);

    /**
     * Suelta la butaca, pero solo si es de esa sesión: soltar la de otro devolvería a la
     * venta algo que alguien está por comprar.
     */
    void liberar(int funcionId, int asientoId, String sesion);

    /** Butacas tomadas en esa función, con la sesión que tiene cada una. */
    Map<Integer, String> bloqueadas(int funcionId);
}
