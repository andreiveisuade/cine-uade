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
import ar.uade.cine.modelo.Clasificacion;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.PeliculaImpl;

/**
 * Misma interfaz, otra tecnología. El gestor no se entera del cambio.
 * try-with-resources cierra Connection y PreparedStatement aunque falle la consulta.
 */
public class PeliculaDAOMySQL implements PeliculaDAO {

    private static final String SELECT_CON_GENEROS =
            "SELECT p.id, p.titulo, p.duracion_minutos, p.clasificacion, p.director, p.sinopsis, "
            + "p.anio, p.idioma_original, p.poster_url, p.en_cartelera, pg.genero "
            + "FROM pelicula p LEFT JOIN pelicula_genero pg ON pg.pelicula_id = p.id";

    @Override
    public void guardar(Pelicula pelicula) {
        String sql = "INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, director, sinopsis, anio, idioma_original, poster_url, en_cartelera) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pelicula.getTitulo());
            ps.setInt(2, pelicula.getDuracionMinutos());
            ps.setString(3, pelicula.getClasificacion().name());
            ps.setString(4, pelicula.getDirector());
            ps.setString(5, pelicula.getSinopsis());
            ps.setInt(6, pelicula.getAnio());
            ps.setString(7, pelicula.getIdiomaOriginal());
            ps.setString(8, pelicula.getPosterUrl());
            ps.setBoolean(9, pelicula.estaEnCartelera());
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
    public void actualizar(Pelicula pelicula) {
        String sql = "UPDATE pelicula SET titulo = ?, duracion_minutos = ?, clasificacion = ?, director = ?, "
                + "sinopsis = ?, anio = ?, idioma_original = ?, poster_url = ?, en_cartelera = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pelicula.getTitulo());
            ps.setInt(2, pelicula.getDuracionMinutos());
            ps.setString(3, pelicula.getClasificacion().name());
            ps.setString(4, pelicula.getDirector());
            ps.setString(5, pelicula.getSinopsis());
            ps.setInt(6, pelicula.getAnio());
            ps.setString(7, pelicula.getIdiomaOriginal());
            ps.setString(8, pelicula.getPosterUrl());
            ps.setBoolean(9, pelicula.estaEnCartelera());
            ps.setInt(10, pelicula.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar la película " + pelicula.getId(), e);
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
                pelicula = new PeliculaImpl(id, rs.getString("titulo"), rs.getInt("duracion_minutos"),
                        Clasificacion.valueOf(rs.getString("clasificacion")));
                pelicula.setDirector(rs.getString("director"));
                pelicula.setSinopsis(rs.getString("sinopsis"));
                pelicula.setAnio(rs.getInt("anio"));
                pelicula.setIdiomaOriginal(rs.getString("idioma_original"));
                pelicula.setPosterUrl(rs.getString("poster_url"));
                pelicula.setEnCartelera(rs.getBoolean("en_cartelera"));
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
