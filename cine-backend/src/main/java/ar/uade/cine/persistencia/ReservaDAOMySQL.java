package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.ReservaImpl;

public class ReservaDAOMySQL implements ReservaDAO {

    private static final String SELECT =
            "SELECT id, funcion_id, cliente_id, cantidad_entradas, estado FROM reserva";

    @Override
    public void guardar(Reserva reserva) {
        String sql = "INSERT INTO reserva (funcion_id, cliente_id, cantidad_entradas, estado) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, reserva.getFuncionId());
            ps.setInt(2, reserva.getClienteId());
            ps.setInt(3, reserva.getCantidadEntradas());
            ps.setString(4, reserva.getEstado().name());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    reserva.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la reserva", e);
        }
    }

    @Override
    public void actualizar(Reserva reserva) {
        String sql = "UPDATE reserva SET funcion_id = ?, cliente_id = ?, cantidad_entradas = ?, estado = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reserva.getFuncionId());
            ps.setInt(2, reserva.getClienteId());
            ps.setInt(3, reserva.getCantidadEntradas());
            ps.setString(4, reserva.getEstado().name());
            ps.setInt(5, reserva.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar la reserva " + reserva.getId(), e);
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la reserva " + id, e);
        }
    }

    @Override
    public List<Reserva> listar() {
        return consultar(SELECT + " ORDER BY id", null, "No se pudieron listar las reservas");
    }

    @Override
    public List<Reserva> listarPorFuncion(int funcionId) {
        return consultar(SELECT + " WHERE funcion_id = ? ORDER BY id", funcionId,
                "No se pudieron listar las reservas de la función " + funcionId);
    }

    @Override
    public List<Reserva> listarPorCliente(int clienteId) {
        return consultar(SELECT + " WHERE cliente_id = ? ORDER BY id", clienteId,
                "No se pudieron listar las reservas del cliente " + clienteId);
    }

    private List<Reserva> consultar(String sql, Integer filtro, String mensajeError) {
        List<Reserva> reservas = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtro != null) {
                ps.setInt(1, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapear(rs));
                }
            }
            return reservas;
        } catch (SQLException e) {
            throw new PersistenciaException(mensajeError, e);
        }
    }

    private Reserva mapear(ResultSet rs) throws SQLException {
        return new ReservaImpl(
                rs.getInt("id"),
                rs.getInt("funcion_id"),
                rs.getInt("cliente_id"),
                rs.getInt("cantidad_entradas"),
                EstadoReserva.valueOf(rs.getString("estado")));
    }
}
