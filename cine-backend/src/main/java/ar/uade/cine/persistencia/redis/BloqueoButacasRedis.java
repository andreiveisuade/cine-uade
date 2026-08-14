package ar.uade.cine.persistencia.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.uade.cine.persistencia.BloqueoButacas;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * El bloqueo efímero, en Redis. Una clave por butaca, con vencimiento propio.
 *
 * <h2>Por qué Redis y no una tabla</h2>
 * <p>Lo que se guarda acá es basura a los tres minutos y no le importa a nadie después: no
 * se audita, no entra en el arqueo y no tiene que sobrevivir a un reinicio. Una tabla lo
 * soportaría, pero habría que barrerla —o filtrar por fecha en cada consulta, como ya hace
 * la expiración de reservas— y quedaría un dato temporal mezclado con los permanentes. El
 * vencimiento por clave es exactamente el mecanismo que hace falta, y Redis lo trae puesto.
 *
 * <h2>Si Redis no responde, el sistema sigue vendiendo</h2>
 * <p>Esta clase <strong>no propaga el fallo de conexión</strong>: lo loguea y contesta como
 * si no hubiera ningún bloqueo. Es lo contrario de lo que hacen los DAO, que tiran
 * {@link ar.uade.cine.persistencia.PersistenciaException}, y la diferencia no es un
 * descuido: <em>un DAO caído significa que el dato no está, y un lock caído significa que
 * nadie la reservó todavía</em>, que es cierto. La primera afirmación no se puede inventar;
 * la segunda es la respuesta correcta.
 *
 * <p>El fondo es que este bloqueo <strong>no es la garantía</strong> de que una butaca no se
 * venda dos veces: esa la sigue dando el {@code UNIQUE (funcion_id, asiento_id)} de MySQL,
 * que arbitra la carrera aunque Redis no exista. Lo que se pierde sin Redis es comodidad
 * —volver a la etapa en que la butaca se pierde recién al confirmar— y no corrección. Hacer
 * que el sistema deje de vender por eso sería voltear algo que hoy funciona sin Redis para
 * proteger algo que la base ya protege.
 *
 * <h2>Una clave por butaca, y no un hash por función</h2>
 * <p>Un hash por función daría un {@code HGETALL} en vez del {@code SCAN} de
 * {@link #bloqueadas(int)}, pero el vencimiento por campo son dos comandos
 * —{@code HSETNX} y {@code HEXPIRE}—, y morirse entre los dos deja una butaca bloqueada
 * <em>para siempre</em>: la única falla que este mecanismo no puede permitirse, porque nadie
 * la limpia después. Con {@code SET NX PX} la butaca y su vencimiento se escriben en un solo
 * comando, así que ese estado no existe. El costo es un {@code SCAN} por consulta, que a la
 * escala de un cine —decenas de claves vivas— no se nota.
 */
public class BloqueoButacasRedis implements BloqueoButacas {

    private static final Logger LOG = LoggerFactory.getLogger(BloqueoButacasRedis.class);

    private static final String PREFIJO = "cine:bloqueo:";

    /**
     * Tomar y renovar en un solo comando. El {@code NX} es lo que hace atómica la carrera:
     * dos sesiones que piden la misma butaca en el mismo instante entran las dos, pero solo
     * una escribe. Sin el script serían un GET y un SET, y entre los dos cabe la otra.
     */
    private static final String TOMAR_O_RENOVAR = """
            if redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then return 1 end
            if redis.call('get', KEYS[1]) == ARGV[1] then
              redis.call('pexpire', KEYS[1], ARGV[2])
              return 1
            end
            return 0
            """;

    /** Borra solo si la butaca es de esa sesión: soltar la de otro la devolvería a la venta. */
    private static final String SOLTAR_SI_ES_MIA = """
            if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end
            return 0
            """;

    private final JedisPooled redis;
    private boolean caido;

    /**
     * Si Redis está caído no se entera acá: el cliente no conecta al construirse, sino en el
     * primer comando. Es a propósito —levantar el backend no puede depender de que Redis ya
     * esté arriba— y es lo mismo que hace {@code ConexionMySQL}, que abre la conexión recién
     * en el primer request.
     */
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
            // Puede venir en null: la clave venció entre el SCAN y el MGET, que es
            // justamente lo que este mecanismo espera que pase.
            if (sesiones.get(i) != null) {
                tomadas.put(asientoDe(claves.get(i)), sesiones.get(i));
            }
        }
        return tomadas;
    }

    /**
     * SCAN y no KEYS: KEYS recorre el espacio de claves entero bloqueando al servidor
     * mientras lo hace, y es el comando que Redis desaconseja fuera de una consola.
     */
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
     * Corre el comando, y si Redis no está devuelve {@code null} para que quien llame
     * conteste lo que contestaría un sistema sin bloqueos.
     *
     * <p>El log avisa una sola vez por caída y otra por vuelta. Una línea por consulta
     * llenaría el archivo justo cuando hay que leerlo, y el estado que importa es el
     * cambio: que Redis está caído hace media hora se dice una vez.
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
