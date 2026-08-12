package ar.uade.cine.persistencia;

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

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.PeliculaImpl;

/**
 * Misma interfaz, otra tecnología. El gestor no se entera del cambio.
 * try-with-resources cierra Connection y PreparedStatement aunque falle la consulta.
 */
public class PeliculaDAOMySQL implements PeliculaDAO {

    private static final String SELECT_CON_GENEROS =
            "SELECT p.id, p.titulo, p.duracion_minutos, pg.genero "
            + "FROM pelicula p LEFT JOIN pelicula_genero pg ON pg.pelicula_id = p.id";

    @Override
    public void guardar(Pelicula pelicula) {
        String sql = "INSERT INTO pelicula (titulo, duracion_minutos) VALUES (?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pelicula.getTitulo());
            ps.setInt(2, pelicula.getDuracionMinutos());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pelicula.setId(claves.getInt(1));
                }
            }
            guardarGeneros(con, pelicula);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la película", e);
        }
    }

    @Override
    public Optional<Pelicula> buscarPorId(int id) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_CON_GENEROS + " WHERE p.id = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pelicula> peliculas = agrupar(rs);
                return peliculas.isEmpty() ? Optional.empty() : Optional.of(peliculas.get(0));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la película " + id, e);
        }
    }

    @Override
    public List<Pelicula> listar() {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_CON_GENEROS + " ORDER BY p.id");
             ResultSet rs = ps.executeQuery()) {

            return agrupar(rs);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron listar las películas", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM pelicula WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la película " + id, e);
        }
    }

    private void guardarGeneros(Connection con, Pelicula pelicula) throws SQLException {
        String sql = "INSERT INTO pelicula_genero (pelicula_id, genero) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Genero genero : pelicula.getGeneros()) {
                ps.setInt(1, pelicula.getId());
                ps.setString(2, genero.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * El JOIN devuelve una fila por cada género, así que la misma película aparece
     * repetida: se agrupa por id y se le van sumando los géneros.
     */
    private List<Pelicula> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, Pelicula> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Pelicula pelicula = porId.get(id);
            if (pelicula == null) {
                pelicula = new PeliculaImpl(id, rs.getString("titulo"), rs.getInt("duracion_minutos"));
                porId.put(id, pelicula);
            }
            String genero = rs.getString("genero");
            if (genero != null) {
                pelicula.agregarGenero(Genero.valueOf(genero));
            }
        }
        return new ArrayList<>(porId.values());
    }
}
