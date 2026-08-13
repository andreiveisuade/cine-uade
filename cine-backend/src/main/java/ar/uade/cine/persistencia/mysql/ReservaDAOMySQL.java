package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.ReservaImpl;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.ButacaOcupadaException;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * La reserva y sus entradas viajan juntas: se guardan en la misma transacción y se leen
 * con un JOIN, agrupando las filas repetidas igual que los géneros de una película.
 * Son dos tablas, así que o entran las dos o no entra ninguna: una reserva sin entradas
 * sería una venta sin butacas.
 *
 * <p>Acá vive además la garantía de que una butaca no se venda dos veces en la misma
 * función. GestorReservas la valida antes para dar un mensaje claro, pero entre esa
 * lectura y esta escritura hay una ventana: dos clientes simultáneos pueden ver la misma
 * butaca libre. Lo que la cierra es el <code>UNIQUE (funcion_id, asiento_id)</code>, que
 * solo la base puede hacer cumplir.
 */
public class ReservaDAOMySQL implements ReservaDAO {

    private static final String SELECT_CON_ENTRADAS =
            "SELECT r.id, r.funcion_id, r.cliente_id, r.estado, r.creada_en, r.codigo, r.ingresada_en, "
            + "e.asiento_id, e.tarifa, e.precio, a.fila, a.numero "
            + "FROM reserva r "
            + "LEFT JOIN entrada e ON e.reserva_id = r.id "
            + "LEFT JOIN asiento a ON a.id = e.asiento_id";

    @Override
    public void guardar(Reserva reserva) {
        String sql = "INSERT INTO reserva (funcion_id, cliente_id, estado, creada_en, codigo) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reserva.getFuncionId());
                ps.setInt(2, reserva.getClienteId());
                ps.setString(3, reserva.getEstado().name());
                ps.setTimestamp(4, Timestamp.valueOf(reserva.getCreadaEn()));
                ps.setString(5, reserva.getCodigo());
                ps.executeUpdate();

                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        reserva.setId(claves.getInt(1));
                    }
                }
                guardarEntradas(con, reserva);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                if (violaUnicidad(e)) {
                    // El UNIQUE (funcion_id, asiento_id) es el arbitro final: si salta, otra
                    // reserva tomo la butaca despues de que GestorReservas la vio libre.
                    throw new ButacaOcupadaException(
                            "Alguien tomó una de esas butacas mientras confirmabas la reserva", e);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la reserva", e);
        }
    }

    /**
     * Si la falla es una violación de unicidad, mirando también las causas encadenadas.
     *
     * <p>No alcanza con {@code catch (SQLIntegrityConstraintViolationException)}: las entradas
     * se insertan con {@code executeBatch()}, y cuando falla un lote JDBC lanza un
     * {@link java.sql.BatchUpdateException}, que hereda de SQLException y no de la de
     * integridad. La violación real queda adentro, como causa. El SQLState 23000 —el estándar
     * para "integrity constraint violation"— sí viaja en la excepción de arriba.
     */
    private boolean violaUnicidad(SQLException error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (causa instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancelar libera las butacas (R6). En la base eso es poner en NULL el
     * <code>funcion_id</code> de sus entradas: dejan de participar del UNIQUE, así que la
     * butaca se puede revender, pero la entrada sigue existiendo con su código y su precio.
     */
    @Override
    public void actualizar(Reserva reserva) {
        String sql = "UPDATE reserva SET estado = ?, ingresada_en = ? WHERE id = ?";
        try (Connection con = ConexionMySQL.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, reserva.getEstado().name());
                ps.setTimestamp(2, reserva.getIngresadaEn() == null
                        ? null : Timestamp.valueOf(reserva.getIngresadaEn()));
                ps.setInt(3, reserva.getId());
                ps.executeUpdate();

                // Cancelada o expirada: en las dos la butaca vuelve a la venta.
                if (!reserva.estaVigente()) {
                    liberarButacas(con, reserva.getId());
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo actualizar la reserva " + reserva.getId(), e);
        }
    }

    private void liberarButacas(Connection con, int reservaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE entrada SET funcion_id = NULL WHERE reserva_id = ?")) {
            ps.setInt(1, reservaId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(int id) {
        List<Reserva> reservas = consultar(SELECT_CON_ENTRADAS + " WHERE r.id = ?", id,
                "No se pudo buscar la reserva " + id);
        return reservas.isEmpty() ? Optional.empty() : Optional.of(reservas.get(0));
    }

    @Override
    public Optional<Reserva> buscarPorCodigo(String codigo) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_CON_ENTRADAS + " WHERE r.codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reserva> reservas = agrupar(rs);
                return reservas.isEmpty() ? Optional.empty() : Optional.of(reservas.get(0));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la reserva por codigo", e);
        }
    }

    @Override
    public List<Reserva> listar() {
        return consultar(SELECT_CON_ENTRADAS + " ORDER BY r.id", null,
                "No se pudieron listar las reservas");
    }

    @Override
    public List<Reserva> listarPorFuncion(int funcionId) {
        return consultar(SELECT_CON_ENTRADAS + " WHERE r.funcion_id = ? ORDER BY r.id", funcionId,
                "No se pudieron listar las reservas de la función " + funcionId);
    }

    @Override
    public List<Reserva> listarPorCliente(int clienteId) {
        return consultar(SELECT_CON_ENTRADAS + " WHERE r.cliente_id = ? ORDER BY r.creada_en DESC", clienteId,
                "No se pudieron listar las reservas del cliente " + clienteId);
    }

    /** funcion_id sale de la reserva: no es un dato extra, es lo que sostiene el UNIQUE. */
    private void guardarEntradas(Connection con, Reserva reserva) throws SQLException {
        String sql = "INSERT INTO entrada (reserva_id, asiento_id, funcion_id, tarifa, precio) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Entrada entrada : reserva.getEntradas()) {
                ps.setInt(1, reserva.getId());
                ps.setInt(2, entrada.asientoId());
                ps.setInt(3, reserva.getFuncionId());
                ps.setString(4, entrada.tarifa().name());
                ps.setDouble(5, entrada.precio());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Reserva> consultar(String sql, Integer filtro, String mensajeError) {
        try (Connection con = ConexionMySQL.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtro != null) {
                ps.setInt(1, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return agrupar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(mensajeError, e);
        }
    }

    /** El JOIN trae una fila por entrada: la misma reserva se repite y se le suman las butacas. */
    private List<Reserva> agrupar(ResultSet rs) throws SQLException {
        Map<Integer, Reserva> porId = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Reserva reserva = porId.get(id);
            if (reserva == null) {
                reserva = new ReservaImpl(
                        id,
                        rs.getInt("funcion_id"),
                        rs.getInt("cliente_id"),
                        List.of(),
                        EstadoReserva.valueOf(rs.getString("estado")),
                        rs.getTimestamp("creada_en").toLocalDateTime(),
                        rs.getString("codigo"));
                Timestamp ingreso = rs.getTimestamp("ingresada_en");
                if (ingreso != null) {
                    reserva.setIngresadaEn(ingreso.toLocalDateTime());
                }
                porId.put(id, reserva);
            }
            int asientoId = rs.getInt("asiento_id");
            if (!rs.wasNull()) {
                String codigo = (char) ('A' + rs.getInt("fila") - 1) + String.valueOf(rs.getInt("numero"));
                reserva.agregarEntrada(new Entrada(asientoId, codigo,
                        TipoTarifa.valueOf(rs.getString("tarifa")), rs.getDouble("precio")));
            }
        }
        return new ArrayList<>(porId.values());
    }
}
