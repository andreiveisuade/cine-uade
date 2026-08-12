package ar.uade.cine.interfaces;

import java.util.List;
import java.util.Optional;

public interface ReservaDAO {

    void guardar(Reserva reserva);

    /** Necesario para cambiar el estado a PAGADA o CANCELADA. */
    void actualizar(Reserva reserva);

    Optional<Reserva> buscarPorId(int id);

    List<Reserva> listar();

    /** Para calcular cuántos lugares quedan libres en una función. */
    List<Reserva> listarPorFuncion(int funcionId);

    List<Reserva> listarPorCliente(int clienteId);
}
