package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.PagoImpl;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Misma interfaz, otra tecnología. Un pago no se actualiza ni se borra una vez creado
 * —cobrar es la única operación de escritura—, así que no hay actualizar() en el
 * contrato.
 */
public class PagoDAOMySQL implements PagoDAO {

    private static final String SELECT =
            "SELECT id, reserva_id, monto, medio, fecha, codigo_autorizacion FROM pago";

    @Override
    public void guardar(Pago pago) {
        String sql = "INSERT INTO pago (reserva_id, monto, medio, fecha, codigo_autorizacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pago.getReservaId());
            ps.setDouble(2, pago.getMonto());
            ps.setString(3, pago.getMedio().name());
            ps.setTimestamp(4, Timestamp.valueOf(pago.getFecha()));
            ps.setString(5, pago.getCodigoAutorizacion());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pago.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el pago", e);
        }
    }

    @Override
    public Optional<Pago> buscarPorReserva(int reservaId) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE reserva_id = ?")) {

            ps.setInt(1, reservaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el pago de la reserva " + reservaId, e);
        }
    }

    @Override
    public List<Pago> listarPorFecha(LocalDate fecha) {
        List<Pago> pagos = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE DATE(fecha) = ? ORDER BY fecha")) {

            ps.setDate(1, java.sql.Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapear(rs));
                }
            }
            return pagos;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los pagos del " + fecha, e);
        }
    }

    @Override
    public List<Pago> listar() {
        List<Pago> pagos = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pagos.add(mapear(rs));
            }
            return pagos;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los pagos", e);
        }
    }

    private Pago mapear(ResultSet rs) throws SQLException {
        return new PagoImpl(
                rs.getInt("id"),
                rs.getInt("reserva_id"),
                rs.getDouble("monto"),
                MedioPago.valueOf(rs.getString("medio")),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("codigo_autorizacion"));
    }
}
