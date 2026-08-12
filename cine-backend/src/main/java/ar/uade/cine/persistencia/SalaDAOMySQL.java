package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Sala;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.SalaImpl;

public class SalaDAOMySQL implements SalaDAO {

    @Override
    public void guardar(Sala sala) {
        String sql = "INSERT INTO sala (nombre, capacidad) VALUES (?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sala.getNombre());
            ps.setInt(2, sala.getCapacidad());
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
        String sql = "SELECT id, nombre, capacidad FROM sala WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

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
        String sql = "SELECT id, nombre, capacidad FROM sala ORDER BY id";
        List<Sala> salas = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
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
        return new SalaImpl(rs.getInt("id"), rs.getString("nombre"), rs.getInt("capacidad"));
    }
}
