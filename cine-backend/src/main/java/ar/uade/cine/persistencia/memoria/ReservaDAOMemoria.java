package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Implementación en memoria: sirve para probar la lógica sin levantar MySQL.
 *
 * <p>Es la que le faltaba a la familia. Sin ella, cualquier prueba que tocara una reserva
 * tenía que usar el DAO de archivo, así que escribía en disco de verdad para probar una
 * regla de negocio que no tiene nada que ver con archivos.
 *
 * <p>Igual que el de archivo, no tiene el <code>UNIQUE (funcion_id, asiento_id)</code> que
 * en MySQL impide vender dos veces la misma butaca: esa garantía solo la puede dar la base.
 * Acá alcanza porque corre en un proceso solo.
 */
public class ReservaDAOMemoria implements ReservaDAO {

    private final Map<Integer, Reserva> reservas = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Reserva reserva) {
        reserva.setId(proximoId++);
        reservas.put(reserva.getId(), reserva);
    }

    @Override
    public void actualizar(Reserva reserva) {
        if (!reservas.containsKey(reserva.getId())) {
            throw new PersistenciaException("No existe la reserva " + reserva.getId());
        }
        reservas.put(reserva.getId(), reserva);
    }

    @Override
    public Optional<Reserva> buscarPorId(int id) {
        return Optional.ofNullable(reservas.get(id));
    }

    @Override
    public Optional<Reserva> buscarPorCodigo(String codigo) {
        return reservas.values().stream()
                .filter(r -> r.getCodigo().equals(codigo))
                .findFirst();
    }

    @Override
    public List<Reserva> listar() {
        return new ArrayList<>(reservas.values());
    }

    @Override
    public List<Reserva> listarPorFuncion(int funcionId) {
        return reservas.values().stream().filter(r -> r.getFuncionId() == funcionId).toList();
    }

    @Override
    public List<Reserva> listarPorCliente(int clienteId) {
        return reservas.values().stream().filter(r -> r.getClienteId() == clienteId).toList();
    }
}
