package ar.uade.cine.persistencia.mysql;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * De dónde salen las conexiones a MySQL.
 *
 * <h2>Por qué hay un pool</h2>
 * <p>Antes cada operación abría su propia conexión con {@code DriverManager} y la cerraba
 * al terminar. Abrir una conexión no es gratis: es un handshake TCP más la autenticación
 * del servidor, del orden de milisegundos, contra los microsegundos que tarda la consulta
 * en sí. En una operación suelta no se nota; en el listado de reservas, que arma cada
 * reserva con su función, su sala, su película y su pago, eran cientos de conexiones
 * nuevas para cientos de consultas cortas.
 *
 * <p>El pool las mantiene abiertas y las presta. {@code getConnection()} devuelve una que
 * ya existe, y {@code close()} —el del try-with-resources de {@link Plantilla}— no la
 * cierra: la devuelve. Por eso el código de los DAO no cambia de forma al usarlo.
 *
 * <h2>De dónde salen los datos de conexión</h2>
 * <p>Del entorno, porque el host cambia según dónde corra la app: fuera de Docker la base
 * está en localhost, y adentro el host es el nombre del servicio del compose. Los valores
 * por defecto son los de la máquina de desarrollo, así que sin variables de entorno
 * funciona como siempre.
 */
public final class OrigenMySQL {

    /**
     * Diez conexiones alcanzan y sobran para un cine: el límite real no es la cantidad de
     * usuarios sino cuántas consultas hay simultáneamente en vuelo, y este backend atiende
     * de a un pedido HTTP por hilo. Un pool más grande solo reservaría conexiones ociosas
     * del lado de MySQL.
     */
    private static final int MAXIMO_CONEXIONES = 10;

    private OrigenMySQL() {
    }

    /** El pool de la aplicación. Lo arma {@code Aplicacion} una sola vez. */
    public static DataSource conPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + variable("DB_HOST", "localhost")
                + ":" + variable("DB_PORT", "3306")
                + "/" + variable("DB_NAME", "appsinteractivas"));
        config.setUsername(variable("DB_USER", "root"));
        config.setPassword(variable("DB_PASSWORD", "root"));
        config.setMaximumPoolSize(MAXIMO_CONEXIONES);
        config.setPoolName("cine");
        return new HikariDataSource(config);
    }

    private static String variable(String nombre, String pordefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? pordefecto : valor;
    }
}
