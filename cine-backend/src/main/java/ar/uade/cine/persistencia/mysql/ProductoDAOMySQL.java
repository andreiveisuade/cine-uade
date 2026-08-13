package ar.uade.cine.persistencia.mysql;

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

import ar.uade.cine.dominio.candy.ItemCombo;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.ProductoImpl;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.ProductoDAO;

/**
 * El producto y los componentes de sus combos se leen juntos, con el mismo patrón de
 * agrupación que usa ReservaDAOMySQL con las entradas. El segundo JOIN sobre producto
 * es para traer el nombre de cada componente.
 */
public class ProductoDAOMySQL implements ProductoDAO {

    private static final String SELECT =
            "SELECT p.id, p.nombre, p.tipo, p.precio, p.disponible, "
            + "ci.producto_id AS componente_id, ci.cantidad, c.nombre AS componente_nombre "
            + "FROM producto p "
            + "LEFT JOIN combo_item ci ON ci.combo_id = p.id "
            + "LEFT JOIN producto c ON c.id = ci.producto_id";

    @Override
    public void guardar(Producto producto) {
        String sql = "INSERT INTO producto (nombre, tipo, precio, disponible) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, producto.getNombre());
                ps.setString(2, producto.getTipo().name());
                ps.setDouble(3, producto.getPrecio());
                ps.setBoolean(4, producto.estaDisponible());
                ps.executeUpdate();

                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        producto.setId(claves.getInt(1));
                    }
                }
                guardarComponentes(con, producto);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el producto", e);
        }
    }

    /**
     * Reemplaza también los componentes si es un combo: son parte del producto, y
     * actualizarlo sin tocarlos dejaría para siempre los que tenía al armarse. Nombre y
     * tipo no se actualizan porque el dominio no los deja cambiar.
     */
    @Override
    public void actualizar(Producto producto) {
        String sql = "UPDATE producto SET precio = ?, disponible = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, producto.getPrecio());
                ps.setBoolean(2, producto.estaDisponible());
                ps.setInt(3, producto.getId());
                ps.executeUpdate();

                borrarComponentes(con, producto.getId());
                guardarComponentes(con, producto);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar el producto " + producto.getId(), e);
        }
    }

    private void borrarComponentes(Connection con, int comboId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM combo_item WHERE combo_id = ?")) {
            ps.setInt(1, comboId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        List<Producto> productos = consultar(SELECT + " WHERE p.id = ?", id,
                "No se pudo buscar el producto " + id);
        return productos.isEmpty() ? Optional.empty() : Optional.of(productos.get(0));
    }

    @Override
    public List<Producto> listar() {
        return consultar(SELECT + " ORDER BY p.id", null, "No se pudieron listar los productos");
    }

    @Override
    public List<Producto> listarDisponibles() {
        return consultar(SELECT + " WHERE p.disponible = TRUE ORDER BY p.id", null,
                "No se pudieron listar los productos disponibles");
    }

    private void guardarComponentes(Connection con, Producto combo) throws SQLException {
        if (combo.getComponentes().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO combo_item (combo_id, producto_id, cantidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ItemCombo componente : combo.getComponentes()) {
                ps.setInt(1, combo.getId());
                ps.setInt(2, componente.productoId());
                ps.setInt(3, componente.cantidad());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Producto> consultar(String sql, Integer filtro, String mensajeError) {
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

    /** El JOIN devuelve una fila por cada componente: se agrupa por id de producto. */
    private List<Producto> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, Producto> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Producto producto = porId.get(id);
            if (producto == null) {
                producto = new ProductoImpl(
                        id,
                        rs.getString("nombre"),
                        TipoProducto.valueOf(rs.getString("tipo")),
                        rs.getDouble("precio"),
                        rs.getBoolean("disponible"));
                porId.put(id, producto);
            }
            int componenteId = rs.getInt("componente_id");
            if (!rs.wasNull()) {
                producto.agregarComponente(new ItemCombo(
                        componenteId, rs.getString("componente_nombre"), rs.getInt("cantidad")));
            }
        }
        return new ArrayList<>(porId.values());
    }
}
