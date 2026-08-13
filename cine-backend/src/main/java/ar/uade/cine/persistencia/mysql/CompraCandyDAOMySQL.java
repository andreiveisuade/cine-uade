package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.CompraCandyImpl;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Misma interfaz, otra tecnología. Una compra vive en dos tablas (compra_candy e
 * item_compra), así que guardarla es una transacción, igual que PeliculaDAOMySQL con
 * los géneros. item_compra copia nombre y precio_unitario en vez de solo referenciar
 * producto_id: si el precio del producto cambia después, el ticket ya emitido no
 * tiene que cambiar con él.
 */
public class CompraCandyDAOMySQL implements CompraCandyDAO {

    private static final String SELECT =
            "SELECT c.id, c.cliente_id, c.reserva_id, c.fecha, c.medio, c.codigo_autorizacion, "
            + "i.producto_id, i.nombre, i.cantidad, i.precio_unitario "
            + "FROM compra_candy c "
            + "LEFT JOIN item_compra i ON i.compra_id = c.id";

    @Override
    public void guardar(CompraCandy compra) {
        String sql = "INSERT INTO compra_candy (cliente_id, reserva_id, fecha, medio, codigo_autorizacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // setObject y no setInt: los dos admiten NULL, que es como se guarda una
                // venta de mostrador sin cliente ni reserva detrás.
                ps.setObject(1, compra.getClienteId(), java.sql.Types.INTEGER);
                ps.setObject(2, compra.getReservaId(), java.sql.Types.INTEGER);
                ps.setTimestamp(3, Timestamp.valueOf(compra.getFecha()));
                ps.setString(4, compra.getMedio().name());
                ps.setString(5, compra.getCodigoAutorizacion());
                ps.executeUpdate();

                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        compra.setId(claves.getInt(1));
                    }
                }
                guardarItems(con, compra);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la compra del candy", e);
        }
    }

    @Override
    public Optional<CompraCandy> buscarPorId(int id) {
        List<CompraCandy> compras = consultar(SELECT + " WHERE c.id = ?", id,
                "No se pudo buscar la compra " + id);
        return compras.isEmpty() ? Optional.empty() : Optional.of(compras.get(0));
    }

    @Override
    public List<CompraCandy> listarPorFecha(LocalDate fecha) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(
                     SELECT + " WHERE DATE(c.fecha) = ? ORDER BY c.fecha")) {

            ps.setDate(1, java.sql.Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                return agrupar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar las compras del " + fecha, e);
        }
    }

    @Override
    public List<CompraCandy> listarPorCliente(int clienteId) {
        return consultar(SELECT + " WHERE c.cliente_id = ? ORDER BY c.fecha DESC", clienteId,
                "No se pudieron listar las compras del cliente " + clienteId);
    }

    private void guardarItems(Connection con, CompraCandy compra) throws SQLException {
        String sql = "INSERT INTO item_compra (compra_id, producto_id, nombre, cantidad, precio_unitario) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ItemCompra item : compra.getItems()) {
                ps.setInt(1, compra.getId());
                ps.setInt(2, item.productoId());
                ps.setString(3, item.nombre());
                ps.setInt(4, item.cantidad());
                ps.setDouble(5, item.precioUnitario());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<CompraCandy> consultar(String sql, Integer filtro, String mensajeError) {
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

    /** El JOIN devuelve una fila por cada item: se agrupa por id de compra. */
    private List<CompraCandy> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, CompraCandy> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            CompraCandy compra = porId.get(id);
            if (compra == null) {
                compra = new CompraCandyImpl(
                        id,
                        (Integer) rs.getObject("cliente_id"),
                        (Integer) rs.getObject("reserva_id"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        MedioPago.valueOf(rs.getString("medio")),
                        rs.getString("codigo_autorizacion"),
                        List.of());
                porId.put(id, compra);
            }
            int productoId = rs.getInt("producto_id");
            if (!rs.wasNull()) {
                compra.agregarItem(new ItemCompra(productoId, rs.getString("nombre"),
                        rs.getInt("cantidad"), rs.getDouble("precio_unitario")));
            }
        }
        return new ArrayList<>(porId.values());
    }
}
