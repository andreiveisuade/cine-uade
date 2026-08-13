package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.dominio.programaciones.ProgramacionImpl;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.ProgramacionDAO;

/**
 * Los días de la grilla van en tabla hija y no como lista separada por comas, por lo
 * mismo que promocion_dia y pelicula_genero: una columna con "MONDAY,WEDNESDAY" adentro
 * no se puede consultar ni indexar. Sin filas significa toda la semana.
 *
 * <p>Por eso el alta necesita transacción: la grilla y sus días son un solo hecho, y una
 * programación a la que le faltan días generaría funciones que nadie pidió.
 */
public class ProgramacionDAOMySQL implements ProgramacionDAO {

    private static final String SELECT =
            "SELECT id, pelicula_id, sala_id, desde, hasta, hora_inicio, version, proyeccion, "
            + "precio, activa, generada_hasta FROM programacion";

    @Override
    public void guardar(Programacion programacion) {
        String sql = "INSERT INTO programacion (pelicula_id, sala_id, desde, hasta, hora_inicio, "
                + "version, proyeccion, precio, activa, generada_hasta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, programacion.getPeliculaId());
                ps.setInt(2, programacion.getSalaId());
                ps.setDate(3, Date.valueOf(programacion.getDesde()));
                // hasta null es una grilla abierta: corre hasta que la den de baja.
                ps.setDate(4, fecha(programacion.getHasta()));
                ps.setTime(5, Time.valueOf(programacion.getHoraInicio()));
                ps.setString(6, programacion.getVersion().name());
                ps.setString(7, programacion.getProyeccion().name());
                ps.setDouble(8, programacion.getPrecio());
                ps.setBoolean(9, programacion.estaActiva());
                ps.setDate(10, fecha(programacion.getGeneradaHasta()));
                ps.executeUpdate();

                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        programacion.setId(claves.getInt(1));
                    }
                }
                guardarDias(con, programacion);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la programación", e);
        }
    }

    /**
     * Solo cambia si está activa. El patrón temporal no se edita: cambiarlo dejaría a las
     * funciones ya generadas contradiciendo a la grilla que dice haberlas creado.
     */
    @Override
    public void actualizar(Programacion programacion) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE programacion SET activa = ?, generada_hasta = ? WHERE id = ?")) {

            ps.setBoolean(1, programacion.estaActiva());
            ps.setDate(2, fecha(programacion.getGeneradaHasta()));
            ps.setInt(3, programacion.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo actualizar la programación " + programacion.getId(), e);
        }
    }

    private void guardarDias(Connection con, Programacion programacion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO programacion_dia (programacion_id, dia) VALUES (?, ?)")) {
            for (DayOfWeek dia : programacion.getDiasSemana()) {
                ps.setInt(1, programacion.getId());
                ps.setString(2, dia.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public Optional<Programacion> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la programación " + id, e);
        }
    }

    @Override
    public List<Programacion> listar() {
        return consultar(SELECT + " ORDER BY desde, hora_inicio",
                "No se pudieron listar las programaciones");
    }

    @Override
    public List<Programacion> listarActivas() {
        return consultar(SELECT + " WHERE activa = TRUE ORDER BY desde, hora_inicio",
                "No se pudieron listar las programaciones activas");
    }

    private List<Programacion> consultar(String sql, String mensajeError) {
        List<Programacion> programaciones = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                programaciones.add(mapear(con, rs));
            }
            return programaciones;
        } catch (SQLException e) {
            throw new PersistenciaException(mensajeError, e);
        }
    }

    private Programacion mapear(Connection con, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Programacion programacion = new ProgramacionImpl(
                id,
                rs.getInt("pelicula_id"),
                rs.getInt("sala_id"),
                rs.getDate("desde").toLocalDate(),
                fechaDe(rs, "hasta"),
                rs.getTime("hora_inicio").toLocalTime(),
                diasDe(con, id),
                Version.valueOf(rs.getString("version")),
                Proyeccion.valueOf(rs.getString("proyeccion")),
                rs.getDouble("precio"));
        programacion.setActiva(rs.getBoolean("activa"));
        programacion.setGeneradaHasta(fechaDe(rs, "generada_hasta"));
        return programacion;
    }

    /** Las fechas que admiten null viajan asi en las dos direcciones. */
    private static Date fecha(LocalDate valor) {
        return valor == null ? null : Date.valueOf(valor);
    }

    private static LocalDate fechaDe(ResultSet rs, String columna) throws SQLException {
        Date valor = rs.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }

    private Set<DayOfWeek> diasDe(Connection con, int programacionId) throws SQLException {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT dia FROM programacion_dia WHERE programacion_id = ?")) {
            ps.setInt(1, programacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dias.add(DayOfWeek.valueOf(rs.getString("dia")));
                }
            }
        }
        return dias;
    }
}
