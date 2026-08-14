package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import ar.uade.cine.persistencia.PersistenciaException;

/**
 * El andamiaje de JDBC, escrito una sola vez.
 *
 * <h2>Por qué existe</h2>
 * <p>Los trece DAO de MySQL hacían todos la misma danza: abrir la conexión, preparar la
 * sentencia, poner los parámetros, recorrer el {@code ResultSet}, cerrar todo en el orden
 * correcto y traducir la {@code SQLException}. De esas seis cosas, <strong>una sola</strong>
 * es del DAO —qué SQL y cómo se lee una fila—; las otras cinco son siempre iguales. Estaban
 * copiadas 59 veces, y cada consulta nueva empezaba por copiar la de al lado.
 *
 * <p>Es una <em>fabricación pura</em> en el sentido de GRASP: no representa nada del cine.
 * Existe porque asignarle a cada DAO la responsabilidad de manejar JDBC producía esa
 * repetición, y sacarla acá deja a los DAO diciendo únicamente lo suyo: la consulta y el
 * mapeo. Un DAO pasa de ~95 líneas a ~40, y el bloque {@code try/catch} que traduce errores
 * está en un lugar y no en cincuenta y nueve.
 *
 * <h2>La conexión entra, no se busca</h2>
 * <p>Recibe un {@link DataSource} por constructor en vez de llamar a un método estático.
 * Antes cada DAO invocaba {@code ConexionMySQL.abrir()}, o sea que dependía de una clase
 * concreta y global: no se podía apuntar a otra base sin tocar el código, y cada operación
 * abría una conexión nueva y la tiraba. Con el origen inyectado, quién provee las
 * conexiones —un pool, una base de prueba— se decide en {@code Aplicacion}, que es donde
 * ya se deciden todas las demás implementaciones.
 *
 * <h2>Qué NO hace</h2>
 * <p>No arma SQL. Las consultas siguen escritas a mano en cada DAO, como strings visibles:
 * esto es plomería, no un ORM. Y no interpreta el negocio: si algo falla, lo envuelve en
 * {@link PersistenciaException} con el mensaje que le pasa el DAO, porque el DAO es el que
 * sabe qué se estaba intentando hacer.
 */
public class Plantilla {

    private final DataSource origen;

    public Plantilla(DataSource origen) {
        this.origen = origen;
    }

    /** Lo que el DAO pone en los signos de pregunta. */
    @FunctionalInterface
    public interface Parametros {
        void poner(PreparedStatement sentencia) throws SQLException;

        /** Para las consultas sin parámetros, que igual pasan por el mismo camino. */
        Parametros NINGUNO = sentencia -> { };
    }

    /** Cómo se lee una fila. Es lo único que sabe el DAO y no sabe esta clase. */
    @FunctionalInterface
    public interface Fila<T> {
        T leer(ResultSet resultado) throws SQLException;
    }

    /** Varias sentencias que tienen que entrar o no entrar juntas. */
    @FunctionalInterface
    public interface Trabajo {
        void hacer(Connection conexion) throws SQLException;
    }

    /**
     * El resultado entero, sin posicionar. Lo necesitan los DAO cuyo SELECT trae un JOIN
     * donde <strong>una entidad ocupa varias filas</strong> —un producto con sus
     * componentes, una reserva con sus entradas— y hay que recorrer todo para agrupar.
     *
     * <p>Es distinto de {@link Fila}, que recibe el resultado ya parado en una fila y
     * devuelve un objeto. Acá el que agrupa maneja el {@code next()}.
     */
    @FunctionalInterface
    public interface Lector<T> {
        T leer(ResultSet resultado) throws SQLException;
    }

    /** Varias consultas de lectura sobre la misma conexión. */
    @FunctionalInterface
    public interface Lectura<T> {
        T hacer(Connection conexion) throws SQLException;
    }

    /**
     * Varias lecturas encadenadas sobre una sola conexión, para armar una entidad que
     * vive en más de una tabla y no se puede traer con un JOIN.
     *
     * <p>Lo usa {@code PromocionDAOMySQL}: cada promoción necesita sus días y sus medios
     * de pago, que están en dos tablas hijas. Sin esto, cada promoción del listado pediría
     * su propia conexión al pool para leer dos filas.
     *
     * <p>No abre transacción: son lecturas. Para escrituras que tienen que entrar juntas
     * está {@link #enTransaccion}.
     */
    public <T> T leyendo(Lectura<T> lectura, String queFallo) {
        try (Connection con = origen.getConnection()) {
            return lectura.hacer(con);
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    /** Una consulta cuyo resultado se procesa entero de una vez, para agrupar un JOIN. */
    public <T> T consultar(String sql, Parametros parametros, Lector<T> lector, String queFallo) {
        try (Connection con = origen.getConnection();
             PreparedStatement sentencia = con.prepareStatement(sql)) {

            parametros.poner(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return lector.leer(resultado);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    public <T> List<T> listar(String sql, Parametros parametros, Fila<T> fila, String queFallo) {
        try (Connection con = origen.getConnection();
             PreparedStatement sentencia = con.prepareStatement(sql)) {

            parametros.poner(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                List<T> filas = new ArrayList<>();
                while (resultado.next()) {
                    filas.add(fila.leer(resultado));
                }
                return filas;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    public <T> List<T> listar(String sql, Fila<T> fila, String queFallo) {
        return listar(sql, Parametros.NINGUNO, fila, queFallo);
    }

    /**
     * La primera fila, o vacío si no hubo ninguna. Devuelve {@code Optional} y no
     * {@code null} porque "no está" es una respuesta normal de una búsqueda por id, y el
     * que llama tiene que verse obligado a contemplarla.
     */
    public <T> Optional<T> buscarUno(String sql, Parametros parametros, Fila<T> fila,
                                     String queFallo) {
        try (Connection con = origen.getConnection();
             PreparedStatement sentencia = con.prepareStatement(sql)) {

            parametros.poner(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(fila.leer(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    /** Un INSERT, UPDATE o DELETE sin id que recuperar. */
    public int ejecutar(String sql, Parametros parametros, String queFallo) {
        try (Connection con = origen.getConnection();
             PreparedStatement sentencia = con.prepareStatement(sql)) {

            parametros.poner(sentencia);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    /**
     * Un INSERT que devuelve el id que asignó MySQL con AUTO_INCREMENT. Es lo que deja a
     * la entidad recién guardada con su identidad puesta, para poder seguir operando con
     * ella sin volver a leerla.
     */
    public int insertar(String sql, Parametros parametros, String queFallo) {
        try (Connection con = origen.getConnection();
             PreparedStatement sentencia =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            parametros.poner(sentencia);
            sentencia.executeUpdate();
            return idGenerado(sentencia);
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    /**
     * Varias sentencias en una transacción: entran todas o no entra ninguna.
     *
     * <p>Lo necesitan las entidades que viven en más de una tabla —una película con sus
     * géneros, una reserva con sus entradas, un combo con sus componentes—. Guardar la
     * cabecera y fallar en el detalle dejaría media entidad en la base, que es peor que no
     * haber guardado nada.
     *
     * <p>Es el único método que le presta la {@link Connection} al DAO, justamente porque
     * es el único caso donde el DAO necesita encadenar sentencias sobre la misma.
     */
    public void enTransaccion(Trabajo trabajo, String queFallo) {
        try (Connection con = origen.getConnection()) {
            con.setAutoCommit(false);
            try {
                trabajo.hacer(con);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                // También RuntimeException, y no solo SQLException: el DAO de reservas
                // traduce la violación del UNIQUE a ButacaOcupadaException adentro del
                // trabajo. Si esa no disparara el rollback, el setAutoCommit(true) de acá
                // abajo confirmaría una transacción a medias.
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(queFallo, e);
        }
    }

    /** El id que acaba de asignar la base, o 0 si la sentencia no generó ninguno. */
    public static int idGenerado(PreparedStatement sentencia) throws SQLException {
        try (ResultSet claves = sentencia.getGeneratedKeys()) {
            return claves.next() ? claves.getInt(1) : 0;
        }
    }
}
