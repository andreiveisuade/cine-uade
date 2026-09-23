package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.util.Map;

public interface BloqueoButacas {

    boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion);

    void liberar(int funcionId, int asientoId, String sesion);

    Map<Integer, String> bloqueadas(int funcionId);
}
