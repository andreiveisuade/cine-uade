package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.CompraCandyImpl;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.persistencia.CompraCandyDAO;

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

    private final Plantilla plantilla;

    public CompraCandyDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(CompraCandy compra) {
        plantilla.enTransaccion(con -> {
            String sql = "INSERT INTO compra_candy (cliente_id, reserva_id, fecha, medio, codigo_autorizacion) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // setObject y no setInt: los dos admiten NULL, que es como se guarda una
                // venta de mostrador sin cliente ni reserva detrás.
                ps.setObject(1, compra.getClienteId(), java.sql.Types.INTEGER);
                ps.setObject(2, compra.getReservaId(), java.sql.Types.INTEGER);
                ps.setTimestamp(3, Timestamp.valueOf(compra.getFecha()));
                ps.setString(4, compra.getMedio().name());
                ps.setString(5, compra.getCodigoAutorizacion());
                ps.executeUpdate();
                compra.setId(Plantilla.idGenerado(ps));
            }
            guardarItems(con, compra);
        }, "No se pudo guardar la compra del candy");
    }

    @Override
    public Optional<CompraCandy> buscarPorId(int id) {
        List<CompraCandy> compras = plantilla.consultar(SELECT + " WHERE c.id = ?",
                ps -> ps.setInt(1, id), CompraCandyDAOMySQL::agrupar,
                "No se pudo buscar la compra " + id);
        return compras.isEmpty() ? Optional.empty() : Optional.of(compras.get(0));
    }

    @Override
    public List<CompraCandy> listarPorFecha(LocalDate fecha) {
        return plantilla.consultar(SELECT + " WHERE DATE(c.fecha) = ? ORDER BY c.fecha",
                ps -> ps.setDate(1, java.sql.Date.valueOf(fecha)), CompraCandyDAOMySQL::agrupar,
                "No se pudieron listar las compras del " + fecha);
    }

    @Override
    public List<CompraCandy> listarPorCliente(int clienteId) {
        return plantilla.consultar(SELECT + " WHERE c.cliente_id = ? ORDER BY c.fecha DESC",
                ps -> ps.setInt(1, clienteId), CompraCandyDAOMySQL::agrupar,
                "No se pudieron listar las compras del cliente " + clienteId);
    }

    @Override
    public List<CompraCandy> listarPorReserva(int reservaId) {
        return plantilla.consultar(SELECT + " WHERE c.reserva_id = ? ORDER BY c.fecha",
                ps -> ps.setInt(1, reservaId), CompraCandyDAOMySQL::agrupar,
                "No se pudieron listar las compras de la reserva " + reservaId);
    }

    private static void guardarItems(Connection con, CompraCandy compra) throws SQLException {
        String sql = "INSERT INTO item_compra (compra_id, producto_id, nombre, cantidad, precio_unitario) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ItemCompra item : compra.getItems()) {
                ps.setInt(1, compra.getId());
                ps.setInt(2, item.productoId());
                ps.setString(3, item.nombre());
                ps.setInt(4, item.cantidad());
                ps.setDouble(5, item.precioUnitario().aPesos());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * El JOIN devuelve una fila por cada item: se agrupa por id de compra.
     *
     * <p>Los items se juntan primero y la compra se construye al final, ya completa. Así
     * {@code CompraCandy} no necesita publicar un <code>agregarItem</code> que solo servía
     * para este armado: una venta cerrada no admite items nuevos.
     */
    private static List<CompraCandy> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, FilasDeCompra> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            FilasDeCompra filas = porId.get(id);
            if (filas == null) {
                filas = new FilasDeCompra(
                        id,
                        (Integer) rs.getObject("cliente_id"),
                        (Integer) rs.getObject("reserva_id"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        MedioPago.valueOf(rs.getString("medio")),
                        rs.getString("codigo_autorizacion"),
                        new ArrayList<>());
                porId.put(id, filas);
            }
            int productoId = rs.getInt("producto_id");
            if (!rs.wasNull()) {
                filas.items().add(new ItemCompra(productoId, rs.getString("nombre"),
                        rs.getInt("cantidad"), Dinero.de(rs.getDouble("precio_unitario"))));
            }
        }
        return porId.values().stream().map(FilasDeCompra::aCompra).toList();
    }

    /** Lo leído de una compra mientras se juntan sus filas, antes de poder construirla. */
    private record FilasDeCompra(int id, Integer clienteId, Integer reservaId, LocalDateTime fecha,
                                 MedioPago medio, String codigoAutorizacion,
                                 List<ItemCompra> items) {

        CompraCandy aCompra() {
            return new CompraCandyImpl(id, clienteId, reservaId, fecha, medio,
                    codigoAutorizacion, items);
        }
    }
}
