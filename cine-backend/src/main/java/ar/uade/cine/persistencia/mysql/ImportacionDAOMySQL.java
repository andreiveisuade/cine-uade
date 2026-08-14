package ar.uade.cine.persistencia.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.dominio.cartelera.EstadoImportacion;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dominio.cartelera.ImportacionImpl;
import ar.uade.cine.persistencia.ImportacionDAO;

/**
 * Una tabla sin hijas ni claves foráneas: la importación no apunta a las películas que
 * trajo. Las películas ya están guardadas con su estado de revisión, y repetir acá cuáles
 * fueron sería tener dos versiones del mismo hecho.
 */
public class ImportacionDAOMySQL implements ImportacionDAO {

    private static final String SELECT =
            "SELECT id, estado, paginas, pedida_en, termino_en, nuevas, salteadas, fallidas, "
            + "detalle FROM importacion";

    private final Plantilla plantilla;

    public ImportacionDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Importacion importacion) {
        int id = plantilla.insertar(
                "INSERT INTO importacion (estado, paginas, pedida_en, termino_en, nuevas, "
                + "salteadas, fallidas, detalle) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, importacion.getEstado().name());
                    ps.setInt(2, importacion.getPaginas());
                    ps.setTimestamp(3, Timestamp.valueOf(importacion.getPedidaEn()));
                    ps.setTimestamp(4, momento(importacion.getTerminoEn()));
                    ps.setInt(5, importacion.getNuevas());
                    ps.setInt(6, importacion.getSalteadas());
                    ps.setInt(7, importacion.getFallidas());
                    ps.setString(8, importacion.getDetalle());
                }, "No se pudo guardar la importación");
        importacion.setId(id);
    }

    @Override
    public void actualizar(Importacion importacion) {
        plantilla.ejecutar(
                "UPDATE importacion SET estado = ?, termino_en = ?, nuevas = ?, salteadas = ?, "
                + "fallidas = ?, detalle = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, importacion.getEstado().name());
                    ps.setTimestamp(2, momento(importacion.getTerminoEn()));
                    ps.setInt(3, importacion.getNuevas());
                    ps.setInt(4, importacion.getSalteadas());
                    ps.setInt(5, importacion.getFallidas());
                    ps.setString(6, importacion.getDetalle());
                    ps.setInt(7, importacion.getId());
                }, "No se pudo actualizar la importación " + importacion.getId());
    }

    @Override
    public List<Importacion> listarUltimas(int cuantas) {
        // Por id y no por pedida_en: dos corridas del mismo segundo empatarían, y el id es
        // exactamente el orden en que se pidieron.
        return plantilla.listar(SELECT + " ORDER BY id DESC LIMIT ?",
                ps -> ps.setInt(1, cuantas),
                ImportacionDAOMySQL::mapear,
                "No se pudieron listar las importaciones");
    }

    private static Importacion mapear(ResultSet rs) throws SQLException {
        return new ImportacionImpl(
                rs.getInt("id"),
                rs.getInt("paginas"),
                rs.getTimestamp("pedida_en").toLocalDateTime(),
                EstadoImportacion.valueOf(rs.getString("estado")),
                momentoDe(rs, "termino_en"),
                rs.getInt("nuevas"),
                rs.getInt("salteadas"),
                rs.getInt("fallidas"),
                rs.getString("detalle"));
    }

    /** termino_en es null mientras la corrida no volvió: viaja así en las dos direcciones. */
    private static Timestamp momento(LocalDateTime valor) {
        return valor == null ? null : Timestamp.valueOf(valor);
    }

    private static LocalDateTime momentoDe(ResultSet rs, String columna) throws SQLException {
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }
}
