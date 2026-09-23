package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import ar.uade.cine.infrastructure.reloj.Reloj;


/**
 * Bloqueos en memoria, para que {@code mvn test} pruebe la regla sin Redis. El reloj entra
 * por constructor para probar el vencimiento sin esperar. Sirve para un solo proceso: no
 * reemplaza a Redis en el compose.
 */
public class BloqueoButacasMemoria implements BloqueoButacas {

    private record Bloqueo(String sesion, LocalDateTime vence) {
    }

    private final Map<String, Bloqueo> bloqueos = new LinkedHashMap<>();
    private final Reloj reloj;

    public BloqueoButacasMemoria() {
        this(LocalDateTime::now);
    }

    public BloqueoButacasMemoria(Reloj reloj) {
        this.reloj = reloj;
    }

    @Override
    public boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion) {
        String clave = clave(funcionId, asientoId);
        Bloqueo actual = vigente(clave);
        if (actual != null && !actual.sesion().equals(sesion)) {
            return false;
        }
        bloqueos.put(clave, new Bloqueo(sesion, reloj.ahora().plus(duracion)));
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
        // Copia de las claves: vigente() borra al pasar y daría ConcurrentModificationException.
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

    /** Lo usan los tests: el adaptador es uno solo para toda la suite. */
    public void limpiar() {
        bloqueos.clear();
    }

    /** Saca los vencidos al leer: acá no hay TTL como en Redis ni nadie que limpie. */
    private Bloqueo vigente(String clave) {
        Bloqueo bloqueo = bloqueos.get(clave);
        if (bloqueo == null) {
            return null;
        }
        if (!bloqueo.vence().isAfter(reloj.ahora())) {
            bloqueos.remove(clave);
            return null;
        }
        return bloqueo;
    }

    private static String clave(int funcionId, int asientoId) {
        return funcionId + ":" + asientoId;
    }
}
