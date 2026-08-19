package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Implementación en memoria: sirve para probar el bloqueo sin levantar Redis.
 *
 * <p>Es lo que hace que {@code mvn test} siga corriendo solo. Si la única implementación
 * fuera la de Redis, probar que dos personas no se llevan la misma butaca pediría un
 * servicio levantado, y una regla de negocio dejaría de poder probarse sin infraestructura.
 *
 * <p>El reloj entra por constructor por el mismo motivo que
 * {@link ar.uade.cine.model.ventas.Reserva#estaVencida(LocalDateTime)} recibe el
 * instante: probar que un bloqueo vence no puede costar esperar tres minutos.
 *
 * <p>Sirve además de un solo proceso, así que en el compose no reemplaza a Redis: dos
 * backends detrás del mismo nginx tendrían cada uno su mapa y se ofrecerían las mismas
 * butacas.
 */
public class BloqueoButacasMemoria implements BloqueoButacas {

    /** Quién tiene la butaca y hasta cuándo. */
    private record Bloqueo(String sesion, LocalDateTime vence) {
    }

    private final Map<String, Bloqueo> bloqueos = new LinkedHashMap<>();
    private final Supplier<LocalDateTime> reloj;

    public BloqueoButacasMemoria() {
        this(LocalDateTime::now);
    }

    public BloqueoButacasMemoria(Supplier<LocalDateTime> reloj) {
        this.reloj = reloj;
    }

    @Override
    public boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion) {
        String clave = clave(funcionId, asientoId);
        Bloqueo actual = vigente(clave);
        if (actual != null && !actual.sesion().equals(sesion)) {
            return false;
        }
        bloqueos.put(clave, new Bloqueo(sesion, reloj.get().plus(duracion)));
        return true;
    }

    @Override
    public void liberar(int funcionId, int asientoId, String sesion) {
        String clave = clave(funcionId, asientoId);
        Bloqueo actual = vigente(clave);
        if (actual != null && actual.sesion().equals(sesion)) {
            bloqueos.remove(clave);
        }
    }

    @Override
    public Map<Integer, String> bloqueadas(int funcionId) {
        String prefijo = funcionId + ":";
        Map<Integer, String> tomadas = new LinkedHashMap<>();
        // Copia de las claves: vigente() borra las vencidas al pasar, y recorrer el mapa
        // mientras se lo modifica revienta con ConcurrentModificationException.
        for (String clave : bloqueos.keySet().toArray(new String[0])) {
            if (!clave.startsWith(prefijo)) {
                continue;
            }
            Bloqueo bloqueo = vigente(clave);
            if (bloqueo != null) {
                tomadas.put(Integer.parseInt(clave.substring(prefijo.length())), bloqueo.sesion());
            }
        }
        return tomadas;
    }

    /**
     * El bloqueo si todavía no venció, y de paso lo saca si venció. Redis lo hace solo con
     * el TTL de la clave; acá el vencimiento hay que mirarlo al leer, porque nadie más va a
     * pasar a limpiar.
     */
    private Bloqueo vigente(String clave) {
        Bloqueo bloqueo = bloqueos.get(clave);
        if (bloqueo == null) {
            return null;
        }
        if (!bloqueo.vence().isAfter(reloj.get())) {
            bloqueos.remove(clave);
            return null;
        }
        return bloqueo;
    }

    private static String clave(int funcionId, int asientoId) {
        return funcionId + ":" + asientoId;
    }
}
