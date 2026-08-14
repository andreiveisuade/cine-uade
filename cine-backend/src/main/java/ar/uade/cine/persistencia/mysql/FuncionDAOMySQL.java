package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Misma interfaz, otra tecnología. La función guarda sus asociaciones como
 * pelicula_id/sala_id, no como filas embebidas: por eso no necesita transacción para
 * guardarse, a diferencia de Pelicula.
 */
public class FuncionDAOMySQL implements FuncionDAO {

    private static final String SELECT = "SELECT id, pelicula_id, sala_id, programacion_id, inicio, version, proyeccion, precio FROM funcion";

    private final Plantilla plantilla;

    public FuncionDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Funcion funcion) {
        funcion.setId(plantilla.insertar(
                "INSERT INTO funcion (pelicula_id, sala_id, programacion_id, inicio, version, proyeccion, precio) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, funcion.getPeliculaId());
                    ps.setInt(2, funcion.getSalaId());
                    // null cuando la cargó el administrador a mano: no toda función sale de una grilla.
                    ps.setObject(3, funcion.getProgramacionId());
                    ps.setTimestamp(4, java.sql.Timestamp.valueOf(funcion.getInicio()));
                    ps.setString(5, funcion.getVersion().name());
                    ps.setString(6, funcion.getProyeccion().name());
                    ps.setDouble(7, funcion.getPrecio().aPesos());
                },
                "No se pudo guardar la función"));
    }

    @Override
    public Optional<Funcion> buscarPorId(int id) {
        return plantilla.buscarUno(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id),
                FuncionDAOMySQL::mapear, "No se pudo buscar la función " + id);
    }

    @Override
    public List<Funcion> listar() {
        return plantilla.listar(SELECT + " ORDER BY inicio", FuncionDAOMySQL::mapear,
                "No se pudieron listar las funciones");
    }

    @Override
    public List<Funcion> listarPorPelicula(int peliculaId) {
        return plantilla.listar(SELECT + " WHERE pelicula_id = ? ORDER BY inicio",
                ps -> ps.setInt(1, peliculaId), FuncionDAOMySQL::mapear,
                "No se pudieron listar las funciones de la película " + peliculaId);
    }

    @Override
    public List<Funcion> listarPorSala(int salaId) {
        return plantilla.listar(SELECT + " WHERE sala_id = ? ORDER BY inicio",
                ps -> ps.setInt(1, salaId), FuncionDAOMySQL::mapear,
                "No se pudieron listar las funciones de la sala " + salaId);
    }

    @Override
    public List<Funcion> listarPorProgramacion(int programacionId) {
        return plantilla.listar(SELECT + " WHERE programacion_id = ? ORDER BY inicio",
                ps -> ps.setInt(1, programacionId), FuncionDAOMySQL::mapear,
                "No se pudieron listar las funciones de la programación " + programacionId);
    }

    @Override
    public void eliminar(int id) {
        plantilla.ejecutar("DELETE FROM funcion WHERE id = ?", ps -> ps.setInt(1, id),
                "No se pudo eliminar la función " + id);
    }

    private static Funcion mapear(ResultSet rs) throws SQLException {
        return new FuncionImpl(
                rs.getInt("id"),
                rs.getInt("pelicula_id"),
                rs.getInt("sala_id"),
                rs.getTimestamp("inicio").toLocalDateTime(),
                Version.valueOf(rs.getString("version")),
                Proyeccion.valueOf(rs.getString("proyeccion")),
                Dinero.de(rs.getDouble("precio")),
                // getInt devolvería 0 en vez de null, y la función parecería de la grilla 0.
                (Integer) rs.getObject("programacion_id"));
    }
}
