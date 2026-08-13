package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PersistenciaException;

public class FuncionDAOMySQL implements FuncionDAO {

    private static final String SELECT = "SELECT id, pelicula_id, sala_id, inicio, version, proyeccion, precio FROM funcion";

    @Override
    public void guardar(Funcion funcion) {
        String sql = "INSERT INTO funcion (pelicula_id, sala_id, inicio, version, proyeccion, precio) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, funcion.getPeliculaId());
            ps.setInt(2, funcion.getSalaId());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(funcion.getInicio()));
            ps.setString(4, funcion.getVersion().name());
            ps.setString(5, funcion.getProyeccion().name());
            ps.setDouble(6, funcion.getPrecio());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    funcion.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la función", e);
        }
    }

    @Override
    public Optional<Funcion> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la función " + id, e);
        }
    }

    @Override
    public List<Funcion> listar() {
        return consultar(SELECT + " ORDER BY inicio", null, "No se pudieron listar las funciones");
    }

    @Override
    public List<Funcion> listarPorPelicula(int peliculaId) {
        return consultar(SELECT + " WHERE pelicula_id = ? ORDER BY inicio", peliculaId,
                "No se pudieron listar las funciones de la película " + peliculaId);
    }

    @Override
    public List<Funcion> listarPorSala(int salaId) {
        return consultar(SELECT + " WHERE sala_id = ? ORDER BY inicio", salaId,
                "No se pudieron listar las funciones de la sala " + salaId);
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM funcion WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la función " + id, e);
        }
    }

    /** Los tres listados son la misma consulta con distinto filtro. filtro null = sin WHERE. */
    private List<Funcion> consultar(String sql, Integer filtro, String mensajeError) {
        List<Funcion> funciones = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtro != null) {
                ps.setInt(1, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    funciones.add(mapear(rs));
                }
            }
            return funciones;
        } catch (SQLException e) {
            throw new PersistenciaException(mensajeError, e);
        }
    }

    private Funcion mapear(ResultSet rs) throws SQLException {
        return new FuncionImpl(
                rs.getInt("id"),
                rs.getInt("pelicula_id"),
                rs.getInt("sala_id"),
                rs.getTimestamp("inicio").toLocalDateTime(),
                Version.valueOf(rs.getString("version")),
                Proyeccion.valueOf(rs.getString("proyeccion")),
                rs.getDouble("precio"));
    }
}
