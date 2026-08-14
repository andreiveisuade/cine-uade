package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.EmpleadoImpl;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.persistencia.EmpleadoDAO;

/**
 * Clientes y empleados comparten la tabla usuario: se distinguen por la columna
 * rol, que actúa de discriminador. Este DAO solo ve las filas de empleados, y las
 * reconoce por lo que NO son: todo usuario que no es CLIENTE tiene contraseña y por
 * lo tanto es un empleado. Filtrar por igualdad obligaría a listar cada rol nuevo acá.
 */
public class EmpleadoDAOMySQL implements EmpleadoDAO {

    private static final String SELECT =
            "SELECT id, nombre, email, password_hash, rol FROM usuario WHERE rol <> ?";

    private final Plantilla plantilla;

    public EmpleadoDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Empleado empleado) {
        empleado.setId(plantilla.insertar(
                "INSERT INTO usuario (nombre, email, rol, password_hash) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, empleado.getNombre());
                    ps.setString(2, empleado.getEmail());
                    ps.setString(3, empleado.getRol().name());
                    ps.setString(4, empleado.getPasswordHash());
                },
                "No se pudo guardar el empleado"));
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        return plantilla.buscarUno(SELECT + " AND id = ?",
                ps -> {
                    ps.setString(1, Rol.CLIENTE.name());
                    ps.setInt(2, id);
                },
                EmpleadoDAOMySQL::mapear,
                "No se pudo buscar el empleado " + id);
    }

    @Override
    public Optional<Empleado> buscarPorEmail(String email) {
        return plantilla.buscarUno(SELECT + " AND email = ?",
                ps -> {
                    ps.setString(1, Rol.CLIENTE.name());
                    ps.setString(2, email);
                },
                EmpleadoDAOMySQL::mapear,
                "No se pudo buscar el empleado " + email);
    }

    @Override
    public List<Empleado> listar() {
        return plantilla.listar(SELECT + " ORDER BY id",
                ps -> ps.setString(1, Rol.CLIENTE.name()),
                EmpleadoDAOMySQL::mapear,
                "No se pudieron listar los empleados");
    }

    @Override
    public void eliminar(int id) {
        plantilla.ejecutar("DELETE FROM usuario WHERE id = ? AND rol <> ?",
                ps -> {
                    ps.setInt(1, id);
                    ps.setString(2, Rol.CLIENTE.name());
                },
                "No se pudo eliminar el empleado " + id);
    }

    private static Empleado mapear(ResultSet rs) throws SQLException {
        return new EmpleadoImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Rol.valueOf(rs.getString("rol")));
    }
}
