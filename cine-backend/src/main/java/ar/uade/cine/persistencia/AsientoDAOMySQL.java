package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Asiento;
import ar.uade.cine.interfaces.AsientoDAO;
import ar.uade.cine.modelo.AsientoImpl;
import ar.uade.cine.modelo.TipoAsiento;

public class AsientoDAOMySQL implements AsientoDAO {

    @Override
    public void guardarTodos(List<Asiento> asientos) {
        String sql = "INSERT INTO asiento (sala_id, fila, numero, tipo) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (Asiento asiento : asientos) {
                ps.setInt(1, asiento.getSalaId());
                ps.setInt(2, asiento.getFila());
                ps.setInt(3, asiento.getNumero());
                ps.setString(4, asiento.getTipo().name());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron guardar los asientos", e);
        }
    }

    @Override
    public List<Asiento> listarPorSala(int salaId) {
        String sql = "SELECT id, sala_id, fila, numero, tipo FROM asiento WHERE sala_id = ? ORDER BY fila, numero";
        List<Asiento> asientos = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, salaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    asientos.add(new AsientoImpl(
                            rs.getInt("id"), rs.getInt("sala_id"), rs.getInt("fila"), rs.getInt("numero"),
                            TipoAsiento.valueOf(rs.getString("tipo"))));
                }
            }
            return asientos;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar los asientos de la sala " + salaId, e);
        }
    }
}
