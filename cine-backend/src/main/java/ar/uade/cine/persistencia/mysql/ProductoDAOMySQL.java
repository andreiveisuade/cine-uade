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
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.persistencia.ProductoDAO;

/**
 * El producto y los componentes de sus combos se leen juntos, con el mismo patrón de
 * agrupación que usa ReservaDAOMySQL con las entradas. El segundo JOIN sobre producto
 * es para traer el nombre de cada componente.
 *
 * <p>Guardar y actualizar van en transacción porque el combo vive en dos tablas: dejar
 * el producto sin sus componentes lo convertiría en un combo que no trae nada.
 */
public class ProductoDAOMySQL implements ProductoDAO {

    private static final String SELECT =
            "SELECT p.id, p.nombre, p.tipo, p.precio, p.disponible, "
            + "ci.producto_id AS componente_id, ci.cantidad, c.nombre AS componente_nombre "
            + "FROM producto p "
            + "LEFT JOIN combo_item ci ON ci.combo_id = p.id "
            + "LEFT JOIN producto c ON c.id = ci.producto_id";

    private final Plantilla plantilla;

    public ProductoDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Producto producto) {
        plantilla.enTransaccion(con -> {
            String sql = "INSERT INTO producto (nombre, tipo, precio, disponible) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, producto.getNombre());
                ps.setString(2, producto.getTipo().name());
                ps.setDouble(3, producto.getPrecio().aPesos());
                ps.setBoolean(4, producto.estaDisponible());
                ps.executeUpdate();
                producto.setId(Plantilla.idGenerado(ps));
            }
            guardarComponentes(con, producto);
        }, "No se pudo guardar el producto");
    }

    /**
     * Reemplaza también los componentes si es un combo: son parte del producto, y
     * actualizarlo sin tocarlos dejaría para siempre los que tenía al armarse. Nombre y
     * tipo no se actualizan porque el dominio no los deja cambiar.
     */
    @Override
    public void actualizar(Producto producto) {
        plantilla.enTransaccion(con -> {
            String sql = "UPDATE producto SET precio = ?, disponible = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, producto.getPrecio().aPesos());
                ps.setBoolean(2, producto.estaDisponible());
                ps.setInt(3, producto.getId());
                ps.executeUpdate();
            }
            borrarComponentes(con, producto.getId());
            guardarComponentes(con, producto);
        }, "No se pudo actualizar el producto " + producto.getId());
    }

    private static void borrarComponentes(Connection con, int comboId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM combo_item WHERE combo_id = ?")) {
            ps.setInt(1, comboId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        List<Producto> productos = plantilla.consultar(SELECT + " WHERE p.id = ?",
                ps -> ps.setInt(1, id), ProductoDAOMySQL::agrupar,
                "No se pudo buscar el producto " + id);
        return productos.isEmpty() ? Optional.empty() : Optional.of(productos.get(0));
    }

    @Override
    public List<Producto> listar() {
        return plantilla.consultar(SELECT + " ORDER BY p.id", Plantilla.Parametros.NINGUNO,
                ProductoDAOMySQL::agrupar, "No se pudieron listar los productos");
    }

    @Override
    public List<Producto> listarDisponibles() {
        return plantilla.consultar(SELECT + " WHERE p.disponible = TRUE ORDER BY p.id",
                Plantilla.Parametros.NINGUNO, ProductoDAOMySQL::agrupar,
                "No se pudieron listar los productos disponibles");
    }

    private static void guardarComponentes(Connection con, Producto combo) throws SQLException {
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

    /** El JOIN devuelve una fila por cada componente: se agrupa por id de producto. */
    private static List<Producto> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, Producto> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Producto producto = porId.get(id);
            if (producto == null) {
                producto = new ProductoImpl(
                        id,
                        rs.getString("nombre"),
                        TipoProducto.valueOf(rs.getString("tipo")),
                        Dinero.de(rs.getDouble("precio")),
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
