package ar.uade.cine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.ventas.Reserva;

/**
 * Las reservas, con sus entradas: la relación es del agregado, así que se guardan y se leen
 * juntas sin que nadie tenga que pedirlo.
 *
 * <p>Lo que <strong>no</strong> está acá y sigue estando en la base es la garantía de que una
 * butaca no se venda dos veces en la misma función. {@code GestorReservas} la valida antes
 * para poder dar un mensaje claro, pero entre esa lectura y la escritura hay una ventana:
 * dos clientes simultáneos pueden ver la misma butaca libre. Lo que la cierra es el
 * {@code UNIQUE (funcion_id, asiento_id)} de la tabla entrada, que solo la base puede hacer
 * cumplir. El gestor traduce esa violación a {@link ButacaOcupadaException}.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /**
     * Por el código del QR y no por id: como el cliente no inicia sesión, el código es la
     * única credencial que existe. Con el id se entraría probando números.
     */
    Optional<Reserva> findByCodigo(String codigo);

    List<Reserva> findByFuncionId(int funcionId);

    List<Reserva> findByClienteIdOrderByCreadaEnDesc(int clienteId);
}
