package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.dominio.promociones.PromocionMontoFijo;
import ar.uade.cine.dominio.promociones.PromocionNxM;
import ar.uade.cine.dominio.promociones.PromocionPorcentaje;
import ar.uade.cine.dominio.promociones.TipoPromocion;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Las tres clases de promoción van a la misma tabla con <code>tipo</code> de
 * discriminador, igual que usuario: comparten todas las condiciones y solo cambia cómo
 * calculan el descuento. Las columnas del beneficio son NULL porque cada tipo usa la
 * suya, y es el mismo criterio que hace que password_hash admita NULL en usuario.
 *
 * <p>Los días y los medios de pago van en tablas hijas y no como lista separada por
 * comas, por lo mismo que pelicula_genero: una columna con "MONDAY,WEDNESDAY" adentro no
 * se puede consultar ni indexar. Sin filas significa sin restricción.
 */
public class PromocionDAOMySQL implements PromocionDAO {

    private static final String SELECT =
            "SELECT id, nombre, tipo, porcentaje, monto, lleva, paga, vigencia_desde, "
            + "vigencia_hasta, hora_desde, hora_hasta, activa FROM promocion";

    private final Plantilla plantilla;

    public PromocionDAOMySQL(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Promocion promocion) {
        String sql = "INSERT INTO promocion (nombre, tipo, porcentaje, monto, lleva, paga, "
                + "vigencia_desde, vigencia_hasta, hora_desde, hora_hasta, activa) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        plantilla.enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, promocion.getNombre());
                ps.setString(2, promocion.getTipo().name());
                ps.setObject(3, promocion instanceof PromocionPorcentaje p ? p.getPorcentaje() : null);
                // .aPesos() y no el Dinero pelado: setObject acepta Object, asi que un
                // Dinero acá compila y recién falla contra la base.
                ps.setObject(4, promocion instanceof PromocionMontoFijo m ? m.getMonto().aPesos() : null);
                ps.setObject(5, promocion instanceof PromocionNxM n ? n.getLleva() : null);
                ps.setObject(6, promocion instanceof PromocionNxM n ? n.getPaga() : null);
                ps.setDate(7, Date.valueOf(promocion.getVigenciaDesde()));
                ps.setDate(8, Date.valueOf(promocion.getVigenciaHasta()));
                ps.setTime(9, promocion.getHoraDesde() == null ? null : Time.valueOf(promocion.getHoraDesde()));
                ps.setTime(10, promocion.getHoraHasta() == null ? null : Time.valueOf(promocion.getHoraHasta()));
                ps.setBoolean(11, promocion.estaActiva());
                ps.executeUpdate();
                promocion.setId(Plantilla.idGenerado(ps));
            }
            guardarCondiciones(con, promocion);
        }, "No se pudo guardar la promoción");
    }

    /**
     * Solo cambia lo que el ABM deja tocar: activarla o desactivarla. El beneficio y las
     * condiciones no se editan, se da de baja y se crea otra — si no, un cobro viejo
     * dejaría de poder explicarse con la promoción que dice haber usado.
     */
    @Override
    public void actualizar(Promocion promocion) {
        plantilla.ejecutar("UPDATE promocion SET activa = ? WHERE id = ?", ps -> {
            ps.setBoolean(1, promocion.estaActiva());
            ps.setInt(2, promocion.getId());
        }, "No se pudo actualizar la promoción " + promocion.getId());
    }

    private static void guardarCondiciones(Connection con, Promocion promocion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO promocion_dia (promocion_id, dia) VALUES (?, ?)")) {
            for (DayOfWeek dia : promocion.getDiasSemana()) {
                ps.setInt(1, promocion.getId());
                ps.setString(2, dia.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO promocion_medio (promocion_id, medio) VALUES (?, ?)")) {
            for (MedioPago medio : promocion.getMediosPago()) {
                ps.setInt(1, promocion.getId());
                ps.setString(2, medio.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public Optional<Promocion> buscarPorId(int id) {
        return plantilla.leyendo(con -> {
            try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
                }
            }
        }, "No se pudo buscar la promoción " + id);
    }

    @Override
    public List<Promocion> listar() {
        return consultar(SELECT + " ORDER BY id", "No se pudieron listar las promociones");
    }

    @Override
    public List<Promocion> listarActivas() {
        return consultar(SELECT + " WHERE activa = TRUE ORDER BY id",
                "No se pudieron listar las promociones activas");
    }

    /**
     * Todas las promociones sobre una sola conexión: cada una lee además sus días y sus
     * medios de pago de las tablas hijas, y pedir una conexión por promoción para leer
     * dos filas sería desperdiciar el pool.
     */
    private List<Promocion> consultar(String sql, String mensajeError) {
        return plantilla.leyendo(con -> {
            List<Promocion> promociones = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    promociones.add(mapear(con, rs));
                }
            }
            return promociones;
        }, mensajeError);
    }

    @Override
    public void eliminar(int id) {
        plantilla.ejecutar("DELETE FROM promocion WHERE id = ?", ps -> ps.setInt(1, id),
                "No se pudo eliminar la promoción " + id);
    }

    /** El tipo decide qué implementación construir: es para lo que está el discriminador. */
    private static Promocion mapear(Connection con, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        Set<DayOfWeek> dias = diasDe(con, id);
        Set<MedioPago> medios = mediosDe(con, id);
        LocalTime horaDesde = rs.getTime("hora_desde") == null ? null : rs.getTime("hora_desde").toLocalTime();
        LocalTime horaHasta = rs.getTime("hora_hasta") == null ? null : rs.getTime("hora_hasta").toLocalTime();

        Promocion promocion = switch (TipoPromocion.valueOf(rs.getString("tipo"))) {
            case PORCENTAJE -> new PromocionPorcentaje(nombre, rs.getDouble("porcentaje"),
                    rs.getDate("vigencia_desde").toLocalDate(), rs.getDate("vigencia_hasta").toLocalDate(),
                    dias, horaDesde, horaHasta, medios);
            case MONTO_FIJO -> new PromocionMontoFijo(nombre, Dinero.de(rs.getDouble("monto")),
                    rs.getDate("vigencia_desde").toLocalDate(), rs.getDate("vigencia_hasta").toLocalDate(),
                    dias, horaDesde, horaHasta, medios);
            case NXM -> new PromocionNxM(nombre, rs.getInt("lleva"), rs.getInt("paga"),
                    rs.getDate("vigencia_desde").toLocalDate(), rs.getDate("vigencia_hasta").toLocalDate(),
                    dias, horaDesde, horaHasta, medios);
        };
        promocion.setId(id);
        promocion.setActiva(rs.getBoolean("activa"));
        return promocion;
    }

    private static Set<DayOfWeek> diasDe(Connection con, int promocionId) throws SQLException {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT dia FROM promocion_dia WHERE promocion_id = ?")) {
            ps.setInt(1, promocionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dias.add(DayOfWeek.valueOf(rs.getString("dia")));
                }
            }
        }
        return dias;
    }

    private static Set<MedioPago> mediosDe(Connection con, int promocionId) throws SQLException {
        Set<MedioPago> medios = EnumSet.noneOf(MedioPago.class);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT medio FROM promocion_medio WHERE promocion_id = ?")) {
            ps.setInt(1, promocionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medios.add(MedioPago.valueOf(rs.getString("medio")));
                }
            }
        }
        return medios;
    }
}
