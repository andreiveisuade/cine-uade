package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.EmpleadoImpl;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Clientes y empleados comparten la tabla usuario: se distinguen por la columna
 * rol, que actúa de discriminador. Este DAO solo ve las filas de empleados, y las
 * reconoce por lo que NO son: todo usuario que no es CLIENTE tiene contraseña y por
 * lo tanto es un empleado. Filtrar por igualdad obligaría a listar cada rol nuevo acá.
 */
public class EmpleadoDAOMySQL implements EmpleadoDAO {

    private static final String SELECT =
            "SELECT id, nombre, email, password_hash, rol FROM usuario WHERE rol <> ?";

    @Override
    public void guardar(Empleado empleado) {
        String sql = "INSERT INTO usuario (nombre, email, rol, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, empleado.getNombre());
            ps.setString(2, empleado.getEmail());
            ps.setString(3, empleado.getRol().name());
            ps.setString(4, empleado.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    empleado.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el empleado", e);
        }
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " AND id = ?")) {

            ps.setString(1, Rol.CLIENTE.name());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el empleado " + id, e);
        }
    }

    @Override
    public Optional<Empleado> buscarPorEmail(String email) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " AND email = ?")) {

            ps.setString(1, Rol.CLIENTE.name());
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el empleado " + email, e);
        }
    }

    @Override
    public List<Empleado> listar() {
        List<Empleado> empleados = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY id")) {

            ps.setString(1, Rol.CLIENTE.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    empleados.add(mapear(rs));
                }
            }
            return empleados;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los empleados", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM usuario WHERE id = ? AND rol <> ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setString(2, Rol.CLIENTE.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el empleado " + id, e);
        }
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        return new EmpleadoImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Rol.valueOf(rs.getString("rol")));
    }
}
