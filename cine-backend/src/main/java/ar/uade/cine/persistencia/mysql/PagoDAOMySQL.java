package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.PagoImpl;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Misma interfaz, otra tecnología. Un pago no se actualiza ni se borra una vez creado
 * —cobrar es la única operación de escritura—, así que no hay actualizar() en el
 * contrato.
 */
public class PagoDAOMySQL implements PagoDAO {

    private static final String SELECT =
            "SELECT id, reserva_id, subtotal, promocion_id, descuento, monto, medio, fecha, "
            + "codigo_autorizacion FROM pago";

    private final Plantilla plantilla;

    public PagoDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Pago pago) {
        pago.setId(plantilla.insertar(
                "INSERT INTO pago (reserva_id, subtotal, promocion_id, descuento, monto, "
                        + "medio, fecha, codigo_autorizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, pago.getReservaId());
                    ps.setDouble(2, pago.getSubtotal().aPesos());
                    // NULL cuando no aplicó ninguna promoción, que es el caso normal.
                    ps.setObject(3, pago.getPromocionId(), java.sql.Types.INTEGER);
                    ps.setDouble(4, pago.getDescuento().aPesos());
                    ps.setDouble(5, pago.getMonto().aPesos());
                    ps.setString(6, pago.getMedio().name());
                    ps.setTimestamp(7, Timestamp.valueOf(pago.getFecha()));
                    ps.setString(8, pago.getCodigoAutorizacion());
                },
                "No se pudo guardar el pago"));
    }

    @Override
    public Optional<Pago> buscarPorReserva(int reservaId) {
        return plantilla.buscarUno(SELECT + " WHERE reserva_id = ?", ps -> ps.setInt(1, reservaId),
                PagoDAOMySQL::mapear, "No se pudo buscar el pago de la reserva " + reservaId);
    }

    /**
     * Los pagos de varias reservas en una sola consulta, con un IN armado del tamaño de la
     * lista. Es lo que evita que el listado del panel haga una consulta por fila.
     *
     * <p>Los signos de pregunta se generan según cuántos ids vinieron, y los valores van
     * por parámetro igual que siempre: concatenar los ids en el SQL sería inyección, aunque
     * hoy los ids salgan de la propia base.
     */
    @Override
    public List<Pago> buscarPorReservas(Collection<Integer> reservaIds) {
        if (reservaIds.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = List.copyOf(reservaIds);
        String huecos = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        return plantilla.listar(SELECT + " WHERE reserva_id IN (" + huecos + ")",
                ps -> {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                },
                PagoDAOMySQL::mapear,
                "No se pudieron buscar los pagos de las reservas");
    }

    @Override
    public List<Pago> listarPorFecha(LocalDate fecha) {
        return plantilla.listar(SELECT + " WHERE DATE(fecha) = ? ORDER BY fecha",
                ps -> ps.setDate(1, java.sql.Date.valueOf(fecha)), PagoDAOMySQL::mapear,
                "No se pudieron listar los pagos del " + fecha);
    }

    @Override
    public List<Pago> listar() {
        return plantilla.listar(SELECT + " ORDER BY id", PagoDAOMySQL::mapear,
                "No se pudieron listar los pagos");
    }

    private static Pago mapear(ResultSet rs) throws SQLException {
        return new PagoImpl(
                rs.getInt("id"),
                rs.getInt("reserva_id"),
                Dinero.de(rs.getDouble("subtotal")),
                (Integer) rs.getObject("promocion_id"),
                Dinero.de(rs.getDouble("descuento")),
                MedioPago.valueOf(rs.getString("medio")),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("codigo_autorizacion"));
    }
}
