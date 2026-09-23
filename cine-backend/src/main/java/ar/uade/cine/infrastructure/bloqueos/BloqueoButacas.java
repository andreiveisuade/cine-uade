package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.util.Map;

/**
 * Quién tiene tomada una butaca mientras la elige, antes de que exista la reserva.
 * Todo bloqueo vence solo porque quien cierra la pestaña no avisa. La duración es regla
 * del negocio y la pone {@link ar.uade.cine.service.ventas.Ocupacion}.
 */
public interface BloqueoButacas {

    /**
     * Toma la butaca o renueva el vencimiento si ya era de esa sesión: cada toque del mapa
     * la renueva, así no se pierde a mitad de la compra.
     *
     * @return {@code false} si la tiene otra sesión
     */
    boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion);

    /** Suelta la butaca solo si es de esa sesión. */
    void liberar(int funcionId, int asientoId, String sesion);

    /** Butacas tomadas en esa función, con la sesión que tiene cada una. */
    Map<Integer, String> bloqueadas(int funcionId);
}
