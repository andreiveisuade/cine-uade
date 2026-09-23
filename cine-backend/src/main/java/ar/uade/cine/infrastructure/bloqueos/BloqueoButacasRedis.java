package ar.uade.cine.infrastructure.bloqueos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Bloqueos en Redis, una clave por butaca con TTL: el vencimiento viene puesto. Si Redis no
 * responde contesta "sin bloqueos" y se sigue vendiendo, porque la garantía contra la doble
 * venta es el {@code UNIQUE (funcion_id, asiento_id)} de MySQL. Clave por butaca y no hash
 * por función para que butaca y vencimiento se escriban en un solo comando ({@code SET NX PX}).
 */
public class BloqueoButacasRedis implements BloqueoButacas {

    private static final Logger LOG = LoggerFactory.getLogger(BloqueoButacasRedis.class);

    private static final String PREFIJO = "cine:bloqueo:";

    /** Script para que tomar y renovar sean atómicos: con GET y SET sueltos cabe otra sesión. */
    private static final String TOMAR_O_RENOVAR = """
            if redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then return 1 end
            if redis.call('get', KEYS[1]) == ARGV[1] then
              redis.call('pexpire', KEYS[1], ARGV[2])
              return 1
            end
            return 0
            """;

    private static final String SOLTAR_SI_ES_MIA = """
            if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end
            return 0
            """;

    private final JedisPooled redis;
    private boolean caido;

    /** No conecta al construirse: el backend levanta aunque Redis no esté arriba. */
    public BloqueoButacasRedis() {
        this(variable("REDIS_HOST", "localhost"),
                Integer.parseInt(variable("REDIS_PORT", "6379")));
    }

    public BloqueoButacasRedis(String host, int puerto) {
        this.redis = new JedisPooled(host, puerto);
    }

    private static String variable(String nombre, String pordefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? pordefecto : valor;
    }

    @Override
    public boolean bloquear(int funcionId, int asientoId, String sesion, Duration duracion) {
        Object resultado = intentar(() -> redis.eval(TOMAR_O_RENOVAR,
                List.of(clave(funcionId, asientoId)),
                List.of(sesion, String.valueOf(duracion.toMillis()))));
        // Sin Redis nadie tiene la butaca tomada, así que la respuesta honesta es que sí.
        return resultado == null || Long.valueOf(1).equals(resultado);
    }

    @Override
    public void liberar(int funcionId, int asientoId, String sesion) {
        intentar(() -> redis.eval(SOLTAR_SI_ES_MIA,
                List.of(clave(funcionId, asientoId)), List.of(sesion)));
    }

    @Override
    public Map<Integer, String> bloqueadas(int funcionId) {
        List<String> claves = intentar(() -> clavesDe(funcionId));
        if (claves == null || claves.isEmpty()) {
            return Map.of();
        }
        List<String> sesiones = intentar(() -> redis.mget(claves.toArray(new String[0])));
        if (sesiones == null) {
            return Map.of();
        }
        Map<Integer, String> tomadas = new LinkedHashMap<>();
        for (int i = 0; i < claves.size(); i++) {
            // Null si la clave venció entre el SCAN y el MGET.
            if (sesiones.get(i) != null) {
                tomadas.put(asientoDe(claves.get(i)), sesiones.get(i));
            }
        }
        return tomadas;
    }

    /** SCAN y no KEYS, que bloquea al servidor mientras recorre todas las claves. */
    private List<String> clavesDe(int funcionId) {
        ScanParams parametros = new ScanParams().match(PREFIJO + funcionId + ":*").count(100);
        List<String> claves = new ArrayList<>();
        String cursor = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> pagina = redis.scan(cursor, parametros);
            claves.addAll(pagina.getResult());
            cursor = pagina.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        return claves;
    }

    /**
     * {@code null} si Redis no está, para contestar como sin bloqueos. Loguea solo el cambio
     * de estado, no cada consulta.
     */
    private <T> T intentar(Supplier<T> comando) {
        try {
            T resultado = comando.get();
            if (caido) {
                caido = false;
                LOG.info("redis volvió: las butacas se bloquean de nuevo mientras se eligen");
            }
            return resultado;
        } catch (JedisException e) {
            if (!caido) {
                caido = true;
                LOG.warn("redis no responde: se sigue vendiendo sin bloqueo previo, "
                        + "la doble venta la sigue impidiendo el UNIQUE de la base", e);
            }
            return null;
        }
    }

    private static String clave(int funcionId, int asientoId) {
        return PREFIJO + funcionId + ":" + asientoId;
    }

    private static int asientoDe(String clave) {
        return Integer.parseInt(clave.substring(clave.lastIndexOf(':') + 1));
    }
}
