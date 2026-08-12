package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.interfaces.Entrada;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.modelo.EntradaImpl;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.ReservaImpl;

/**
 * La reserva y sus entradas viajan juntas: se guardan en la misma operación y se leen
 * con un JOIN, agrupando las filas repetidas igual que los géneros de una película.
 */
public class ReservaDAOMySQL implements ReservaDAO {

    private static final String SELECT_CON_ENTRADAS =
            "SELECT r.id, r.funcion_id, r.cliente_id, r.estado, e.asiento_id, a.fila, a.numero "
            + "FROM reserva r "
            + "LEFT JOIN entrada e ON e.reserva_id = r.id "
            + "LEFT JOIN asiento a ON a.id = e.asiento_id";

    @Override
    public void guardar(Reserva reserva) {
        String sql = "INSERT INTO reserva (funcion_id, cliente_id, estado) VALUES (?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, reserva.getFuncionId());
            ps.setInt(2, reserva.getClienteId());
            ps.setString(3, reserva.getEstado().name());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    reserva.setId(claves.getInt(1));
                }
            }
            guardarEntradas(con, reserva);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la reserva", e);
        }
    }

    @Override
    public void actualizar(Reserva reserva) {
        String sql = "UPDATE reserva SET estado = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reserva.getEstado().name());
            ps.setInt(2, reserva.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar la reserva " + reserva.getId(), e);
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(int id) {
        List<Reserva> reservas = consultar(SELECT_CON_ENTRADAS + " WHERE r.id = ?", id,
                "No se pudo buscar la reserva " + id);
        return reservas.isEmpty() ? Optional.empty() : Optional.of(reservas.get(0));
    }

    @Override
    public List<Reserva> listar() {
        return consultar(SELECT_CON_ENTRADAS + " ORDER BY r.id", null,
                "No se pudieron listar las reservas");
    }

    @Override
    public List<Reserva> listarPorFuncion(int funcionId) {
        return consultar(SELECT_CON_ENTRADAS + " WHERE r.funcion_id = ? ORDER BY r.id", funcionId,
                "No se pudieron listar las reservas de la función " + funcionId);
    }

    @Override
    public List<Reserva> listarPorCliente(int clienteId) {
        return consultar(SELECT_CON_ENTRADAS + " WHERE r.cliente_id = ? ORDER BY r.id", clienteId,
                "No se pudieron listar las reservas del cliente " + clienteId);
    }

    private void guardarEntradas(Connection con, Reserva reserva) throws SQLException {
        String sql = "INSERT INTO entrada (reserva_id, asiento_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Entrada entrada : reserva.getEntradas()) {
                ps.setInt(1, reserva.getId());
                ps.setInt(2, entrada.getAsientoId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Reserva> consultar(String sql, Integer filtro, String mensajeError) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtro != null) {
                ps.setInt(1, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return agrupar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(mensajeError, e);
        }
    }

    /** El JOIN trae una fila por entrada: la misma reserva se repite y se le suman las butacas. */
    private List<Reserva> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, Reserva> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Reserva reserva = porId.get(id);
            if (reserva == null) {
                reserva = new ReservaImpl(
                        id,
                        rs.getInt("funcion_id"),
                        rs.getInt("cliente_id"),
                        List.of(),
                        EstadoReserva.valueOf(rs.getString("estado")));
                porId.put(id, reserva);
            }
            int asientoId = rs.getInt("asiento_id");
            if (!rs.wasNull()) {
                String codigo = (char) ('A' + rs.getInt("fila") - 1) + String.valueOf(rs.getInt("numero"));
                reserva.agregarEntrada(new EntradaImpl(asientoId, codigo));
            }
        }
        return new ArrayList<>(porId.values());
    }
}
