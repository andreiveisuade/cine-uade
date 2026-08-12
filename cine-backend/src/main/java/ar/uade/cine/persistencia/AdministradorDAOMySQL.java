package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.AdministradorCine;
import ar.uade.cine.interfaces.AdministradorDAO;
import ar.uade.cine.modelo.AdministradorCineImpl;
import ar.uade.cine.modelo.Rol;

/**
 * Clientes y administradores comparten la tabla usuario: se distinguen por la columna
 * rol, que actúa de discriminador. Este DAO solo ve las filas de administradores.
 */
public class AdministradorDAOMySQL implements AdministradorDAO {

    private static final String SELECT =
            "SELECT id, nombre, email, password_hash FROM usuario WHERE rol = ?";

    @Override
    public void guardar(AdministradorCine administrador) {
        String sql = "INSERT INTO usuario (nombre, email, rol, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, administrador.getNombre());
            ps.setString(2, administrador.getEmail());
            ps.setString(3, Rol.ADMINISTRADOR.name());
            ps.setString(4, administrador.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    administrador.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el administrador", e);
        }
    }

    @Override
    public Optional<AdministradorCine> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " AND id = ?")) {

            ps.setString(1, Rol.ADMINISTRADOR.name());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el administrador " + id, e);
        }
    }

    @Override
    public Optional<AdministradorCine> buscarPorEmail(String email) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " AND email = ?")) {

            ps.setString(1, Rol.ADMINISTRADOR.name());
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el administrador " + email, e);
        }
    }

    @Override
    public List<AdministradorCine> listar() {
        List<AdministradorCine> administradores = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY id")) {

            ps.setString(1, Rol.ADMINISTRADOR.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    administradores.add(mapear(rs));
                }
            }
            return administradores;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los administradores", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM usuario WHERE id = ? AND rol = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setString(2, Rol.ADMINISTRADOR.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el administrador " + id, e);
        }
    }

    private AdministradorCine mapear(ResultSet rs) throws SQLException {
        return new AdministradorCineImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("password_hash"));
    }
}
