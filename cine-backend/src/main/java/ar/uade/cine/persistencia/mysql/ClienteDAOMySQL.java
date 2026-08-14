package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.usuarios.ClienteImpl;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.persistencia.ClienteDAO;

/**
 * Misma interfaz, otra tecnología. Comparte la tabla usuario con EmpleadoDAOMySQL:
 * rol la discrimina, y por eso ninguna consulta acá trae password_hash. El cliente no
 * inicia sesión, así que tampoco tiene actualizar().
 */
public class ClienteDAOMySQL implements ClienteDAO {

    private final Plantilla plantilla;

    public ClienteDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Cliente cliente) {
        cliente.setId(plantilla.insertar(
                "INSERT INTO usuario (nombre, email, rol) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, cliente.getNombre());
                    ps.setString(2, cliente.getEmail());
                    ps.setString(3, Rol.CLIENTE.name());
                },
                "No se pudo guardar el cliente"));
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return plantilla.buscarUno(
                "SELECT id, nombre, email FROM usuario WHERE rol = ? AND id = ?",
                ps -> {
                    ps.setString(1, Rol.CLIENTE.name());
                    ps.setInt(2, id);
                },
                ClienteDAOMySQL::mapear,
                "No se pudo buscar el cliente " + id);
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return plantilla.buscarUno(
                "SELECT id, nombre, email FROM usuario WHERE rol = ? AND email = ?",
                ps -> {
                    ps.setString(1, Rol.CLIENTE.name());
                    ps.setString(2, email);
                },
                ClienteDAOMySQL::mapear,
                "No se pudo buscar el cliente " + email);
    }

    @Override
    public List<Cliente> listar() {
        return plantilla.listar(
                "SELECT id, nombre, email FROM usuario WHERE rol = ? ORDER BY id",
                ps -> ps.setString(1, Rol.CLIENTE.name()),
                ClienteDAOMySQL::mapear,
                "No se pudieron listar los clientes");
    }

    @Override
    public void eliminar(int id) {
        plantilla.ejecutar(
                "DELETE FROM usuario WHERE id = ? AND rol = ?",
                ps -> {
                    ps.setInt(1, id);
                    ps.setString(2, Rol.CLIENTE.name());
                },
                "No se pudo eliminar el cliente " + id);
    }

    private static Cliente mapear(ResultSet rs) throws SQLException {
        return new ClienteImpl(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
    }
}
