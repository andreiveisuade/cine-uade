package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.SalaImpl;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * Misma interfaz, otra tecnología. A diferencia de Pelicula, una sala vive en una sola
 * tabla: no hace falta transacción para guardarla. El id lo asigna MySQL
 * (AUTO_INCREMENT) y se recupera con RETURN_GENERATED_KEYS.
 */
public class SalaDAOMySQL implements SalaDAO {

    private static final String SELECT = "SELECT id, nombre, tipo, minutos_limpieza FROM sala";

    @Override
    public void guardar(Sala sala) {
        String sql = "INSERT INTO sala (nombre, tipo, minutos_limpieza) VALUES (?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sala.getNombre());
            ps.setString(2, sala.getTipo().name());
            ps.setInt(3, sala.getMinutosLimpieza());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    sala.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la sala", e);
        }
    }

    @Override
    public Optional<Sala> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la sala " + id, e);
        }
    }

    @Override
    public List<Sala> listar() {
        List<Sala> salas = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                salas.add(mapear(rs));
            }
            return salas;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar las salas", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM sala WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la sala " + id, e);
        }
    }

    private Sala mapear(ResultSet rs) throws SQLException {
        return new SalaImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                TipoSala.valueOf(rs.getString("tipo")),
                rs.getInt("minutos_limpieza"));
    }
}
