package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.SalaImpl;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * Misma interfaz, otra tecnología. A diferencia de Pelicula, una sala vive en una sola
 * tabla: no hace falta transacción para guardarla. El id lo asigna MySQL
 * (AUTO_INCREMENT) y lo devuelve {@link Plantilla#insertar}.
 *
 * <p>Toda la plomería de JDBC —abrir, cerrar, traducir la SQLException— vive en
 * {@link Plantilla}. Acá queda lo único que es de la sala: sus consultas y cómo se lee
 * una fila.
 */
public class SalaDAOMySQL implements SalaDAO {

    private static final String SELECT = "SELECT id, nombre, tipo, minutos_limpieza FROM sala";

    private final Plantilla plantilla;

    public SalaDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Sala sala) {
        sala.setId(plantilla.insertar(
                "INSERT INTO sala (nombre, tipo, minutos_limpieza) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, sala.getNombre());
                    ps.setString(2, sala.getTipo().name());
                    ps.setInt(3, sala.getMinutosLimpieza());
                },
                "No se pudo guardar la sala"));
    }

    @Override
    public Optional<Sala> buscarPorId(int id) {
        return plantilla.buscarUno(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id),
                SalaDAOMySQL::mapear, "No se pudo buscar la sala " + id);
    }

    @Override
    public List<Sala> listar() {
        return plantilla.listar(SELECT + " ORDER BY id", SalaDAOMySQL::mapear,
                "No se pudieron listar las salas");
    }

    @Override
    public void eliminar(int id) {
        plantilla.ejecutar("DELETE FROM sala WHERE id = ?", ps -> ps.setInt(1, id),
                "No se pudo eliminar la sala " + id);
    }

    private static Sala mapear(ResultSet rs) throws SQLException {
        return new SalaImpl(
                rs.getInt("id"),
                rs.getString("nombre"),
                TipoSala.valueOf(rs.getString("tipo")),
                rs.getInt("minutos_limpieza"));
    }
}
