package ar.uade.cine.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.uade.cine.interfaces.Sala;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.SalaImpl;
import ar.uade.cine.modelo.TipoSala;

/**
 * La distribución se guarda como texto ("8,10,12,12,14") en vez de una tabla aparte:
 * es un dato de la sala que siempre se lee entero y nunca se consulta por partes.
 */
public class SalaDAOMySQL implements SalaDAO {

    private static final String SELECT = "SELECT id, nombre, tipo, butacas_por_fila FROM sala";

    @Override
    public void guardar(Sala sala) {
        String sql = "INSERT INTO sala (nombre, tipo, butacas_por_fila) VALUES (?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sala.getNombre());
            ps.setString(2, sala.getTipo().name());
            ps.setString(3, aTexto(sala.getButacasPorFila()));
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    sala.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la sala", e);
        }
    }

    @Override
    public Optional<Sala> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la sala " + id, e);
        }
    }

    @Override
    public List<Sala> listar() {
        List<Sala> salas = new ArrayList<>();
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                salas.add(mapear(rs));
            }
            return salas;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar las salas", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM sala WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la sala " + id, e);
        }
    }

    private String aTexto(List<Integer> butacasPorFila) {
        return butacasPorFila.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> desdeTexto(String texto) {
        return Arrays.stream(texto.split(",")).map(String::trim).map(Integer::parseInt).toList();
    }

    private Sala mapear(ResultSet rs) throws SQLException {
        return new SalaImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                TipoSala.valueOf(rs.getString("tipo")),
                desdeTexto(rs.getString("butacas_por_fila")));
    }
}
