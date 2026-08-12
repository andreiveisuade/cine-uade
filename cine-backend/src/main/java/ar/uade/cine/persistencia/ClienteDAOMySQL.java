package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Cliente;
import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.modelo.ClienteImpl;

public class ClienteDAOMySQL implements ClienteDAO {

    @Override
    public void guardar(Cliente cliente) {
        String sql = "INSERT INTO cliente (nombre, email) VALUES (?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getEmail());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    cliente.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el cliente", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        String sql = "SELECT id, nombre, email FROM cliente WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el cliente " + id, e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        String sql = "SELECT id, nombre, email FROM cliente WHERE email = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el cliente " + email, e);
        }
    }

    @Override
    public List<Cliente> listar() {
        String sql = "SELECT id, nombre, email FROM cliente ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapear(rs));
            }
            return clientes;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los clientes", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM cliente WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el cliente " + id, e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new ClienteImpl(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
    }
}
