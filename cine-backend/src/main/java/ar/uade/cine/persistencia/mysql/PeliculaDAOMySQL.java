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

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.EstadoRevision;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.cartelera.PeliculaImpl;
import ar.uade.cine.persistencia.PeliculaDAO;

/**
 * Misma interfaz, otra tecnología. El gestor no se entera del cambio.
 * try-with-resources cierra Connection y PreparedStatement aunque falle la consulta.
 *
 * <p>Una película vive en dos tablas, así que guardarla y actualizarla son operaciones
 * multi-tabla: van en una transacción. Sin eso, si fallara el paso de los géneros,
 * quedaría una película sin ninguno y R7 se rompería desde la base.
 */
public class PeliculaDAOMySQL implements PeliculaDAO {

    private static final String SELECT_CON_GENEROS =
            "SELECT p.id, p.titulo, p.duracion_minutos, p.clasificacion, p.director, p.sinopsis, "
            + "p.anio, p.idioma_original, p.poster_url, p.en_cartelera, p.estado_revision, p.puntaje, p.votos, pg.genero "
            + "FROM pelicula p LEFT JOIN pelicula_genero pg ON pg.pelicula_id = p.id";

    private final Plantilla plantilla;

    public PeliculaDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Pelicula pelicula) {
        String sql = "INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, director, sinopsis, anio, idioma_original, poster_url, en_cartelera, estado_revision, puntaje, votos) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        plantilla.enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                cargarDatos(ps, pelicula);
                ps.executeUpdate();
                pelicula.setId(Plantilla.idGenerado(ps));
            }
            guardarGeneros(con, pelicula);
        }, "No se pudo guardar la película");
    }

    /**
     * Reemplaza también los géneros: son parte de la película, y editarla sin tocarlos
     * dejaría para siempre los que tenía al crearse.
     */
    @Override
    public void actualizar(Pelicula pelicula) {
        String sql = "UPDATE pelicula SET titulo = ?, duracion_minutos = ?, clasificacion = ?, director = ?, "
                + "sinopsis = ?, anio = ?, idioma_original = ?, poster_url = ?, en_cartelera = ?, "
                + "estado_revision = ?, puntaje = ?, votos = ? WHERE id = ?";
        plantilla.enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                cargarDatos(ps, pelicula);
                ps.setInt(13, pelicula.getId());
                ps.executeUpdate();
            }
            borrarGeneros(con, pelicula.getId());
            guardarGeneros(con, pelicula);
        }, "No se pudo actualizar la película " + pelicula.getId());
    }

    @Override
    public Optional<Pelicula> buscarPorId(int id) {
        List<Pelicula> peliculas = plantilla.consultar(SELECT_CON_GENEROS + " WHERE p.id = ?",
                ps -> ps.setInt(1, id), PeliculaDAOMySQL::agrupar,
                "No se pudo buscar la película " + id);
        return peliculas.isEmpty() ? Optional.empty() : Optional.of(peliculas.get(0));
    }

    @Override
    public List<Pelicula> listar() {
        return plantilla.consultar(SELECT_CON_GENEROS + " ORDER BY p.id",
                Plantilla.Parametros.NINGUNO, PeliculaDAOMySQL::agrupar,
                "No se pudieron listar las películas");
    }

    /** Los géneros se van solos: pelicula_genero tiene ON DELETE CASCADE. */
    @Override
    public void eliminar(int id) {
        plantilla.ejecutar("DELETE FROM pelicula WHERE id = ?", ps -> ps.setInt(1, id),
                "No se pudo eliminar la película " + id);
    }

    /** Los doce campos de pelicula, en el mismo orden en el INSERT y en el UPDATE. */
    private static void cargarDatos(PreparedStatement ps, Pelicula pelicula) throws SQLException {
        ps.setString(1, pelicula.getTitulo());
        ps.setInt(2, pelicula.getDuracionMinutos());
        ps.setString(3, pelicula.getClasificacion().name());
        ps.setString(4, pelicula.getDirector());
        ps.setString(5, pelicula.getSinopsis());
        ps.setInt(6, pelicula.getAnio());
        ps.setString(7, pelicula.getIdiomaOriginal());
        ps.setString(8, pelicula.getPosterUrl());
        ps.setBoolean(9, pelicula.estaEnCartelera());
        ps.setString(10, pelicula.getEstadoRevision().name());
        ps.setDouble(11, pelicula.getPuntaje());
        ps.setInt(12, pelicula.getVotos());
    }

    private static void guardarGeneros(Connection con, Pelicula pelicula) throws SQLException {
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

    private static void borrarGeneros(Connection con, int peliculaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM pelicula_genero WHERE pelicula_id = ?")) {
            ps.setInt(1, peliculaId);
            ps.executeUpdate();
        }
    }

    /**
     * El JOIN devuelve una fila por cada género, así que la misma película aparece
     * repetida: se agrupa por id y se le van sumando los géneros.
     */
    private static List<Pelicula> agrupar(ResultSet rs) throws SQLException {
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
                pelicula.setEstadoRevision(EstadoRevision.valueOf(rs.getString("estado_revision")));
                pelicula.setPuntaje(rs.getDouble("puntaje"));
                pelicula.setVotos(rs.getInt("votos"));
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
