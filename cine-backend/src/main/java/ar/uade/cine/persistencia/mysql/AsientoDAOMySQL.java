package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.AsientoImpl;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Misma interfaz, otra tecnología. No hay guardar() individual: una sala se crea con
 * todas sus butacas juntas (ver guardarTodos), y lo único que cambia después es el
 * estado de una butaca puntual (fuera de servicio o de vuelta a habilitada).
 */
public class AsientoDAOMySQL implements AsientoDAO {

    @Override
    public void guardarTodos(List<Asiento> asientos) {
        String sql = "INSERT INTO asiento (sala_id, fila, numero, tipo, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

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
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron guardar los asientos", e);
        }
    }

    @Override
    public void actualizar(Asiento asiento) {
        String sql = "UPDATE asiento SET estado = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, asiento.getEstado().name());
            ps.setInt(2, asiento.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar el asiento " + asiento.getId(), e);
        }
    }

    @Override
    public List<Asiento> listarPorSala(int salaId) {
        String sql = "SELECT id, sala_id, fila, numero, tipo, estado FROM asiento WHERE sala_id = ? ORDER BY fila, numero";
        List<Asiento> asientos = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, salaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    asientos.add(new AsientoImpl(
                            rs.getInt("id"), rs.getInt("sala_id"), rs.getInt("fila"), rs.getInt("numero"),
                            TipoAsiento.valueOf(rs.getString("tipo")),
                            EstadoAsiento.valueOf(rs.getString("estado"))));
                }
            }
            return asientos;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los asientos de la sala " + salaId, e);
        }
    }
}
