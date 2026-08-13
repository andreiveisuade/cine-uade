package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Reserva;

/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 *
 * <p>No hay eliminar: una reserva no se borra, cambia de estado (RESERVADA, PAGADA,
 * CANCELADA) con actualizar. Borrarla perdería el historial que necesita el arqueo.
 */
public interface ReservaDAO {

    void guardar(Reserva reserva);

    /** Necesario para cambiar el estado y para registrar el ingreso al cine. */
    void actualizar(Reserva reserva);

    Optional<Reserva> buscarPorId(int id);

    /** Por el código del QR, que es con lo que se valida la entrada en la puerta. */
    Optional<Reserva> buscarPorCodigo(String codigo);

    List<Reserva> listar();

    /** Para calcular cuántos lugares quedan libres en una función. */
    List<Reserva> listarPorFuncion(int funcionId);

    List<Reserva> listarPorCliente(int clienteId);
}
