package ar.uade.cine.persistencia.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.AsientoImpl;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.persistencia.AsientoDAO;

/**
 * Misma interfaz, otra tecnología. No hay guardar() individual: una sala se crea con
 * todas sus butacas juntas (ver guardarTodos), y lo único que cambia después es el
 * estado de una butaca puntual (fuera de servicio o de vuelta a habilitada).
 *
 * <p>Toda la plomería de JDBC vive en {@link Plantilla}. guardarTodos es la excepción:
 * necesita un solo {@code executeBatch()} para cien butacas de una sola vez, algo que
 * ninguno de los métodos de Plantilla ofrece, así que pide prestada la Connection con
 * {@link Plantilla#enTransaccion} para mantener el batch tal como estaba.
 */
public class AsientoDAOMySQL implements AsientoDAO {

    private final Plantilla plantilla;

    public AsientoDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardarTodos(List<Asiento> asientos) {
        String sql = "INSERT INTO asiento (sala_id, fila, numero, tipo, estado) VALUES (?, ?, ?, ?, ?)";
        plantilla.enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                // Una sala puede tener más de cien butacas: batch en vez de un INSERT por fila.
                for (Asiento asiento : asientos) {
                    ps.setInt(1, asiento.getSalaId());
                    ps.setInt(2, asiento.getFila());
                    ps.setInt(3, asiento.getNumero());
                    ps.setString(4, asiento.getTipo().name());
                    ps.setString(5, asiento.getEstado().name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }, "No se pudieron guardar los asientos");
    }

    @Override
    public void actualizar(Asiento asiento) {
        plantilla.ejecutar("UPDATE asiento SET estado = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, asiento.getEstado().name());
                    ps.setInt(2, asiento.getId());
                },
                "No se pudo actualizar el asiento " + asiento.getId());
    }

    @Override
    public List<Asiento> listarPorSala(int salaId) {
        return plantilla.listar(
                "SELECT id, sala_id, fila, numero, tipo, estado FROM asiento WHERE sala_id = ? ORDER BY fila, numero",
                ps -> ps.setInt(1, salaId),
                AsientoDAOMySQL::mapear,
                "No se pudieron listar los asientos de la sala " + salaId);
    }

    private static Asiento mapear(ResultSet rs) throws SQLException {
        return new AsientoImpl(
                rs.getInt("id"), rs.getInt("sala_id"), rs.getInt("fila"), rs.getInt("numero"),
                TipoAsiento.valueOf(rs.getString("tipo")),
                EstadoAsiento.valueOf(rs.getString("estado")));
    }
}
