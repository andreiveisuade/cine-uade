package ar.uade.cine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.ventas.Reserva;

/**
 * R4 no se garantiza acá: el gestor la valida para dar un mensaje claro, pero la carrera
 * entre dos clientes la cierra el UNIQUE (funcion_id, asiento_id) de entrada, que el gestor
 * traduce a {@code ButacaOcupadaException}.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /** Por código y no por id: es la única credencial del cliente, y el id se adivina. */
    Optional<Reserva> findByCodigo(String codigo);

    List<Reserva> findByFuncionId(int funcionId);

    List<Reserva> findByClienteIdOrderByCreadaEnDesc(int clienteId);

    /** R12, sin cargar reservas con sus entradas. */
    boolean existsByFuncionId(int funcionId);

    boolean existsByClienteId(int clienteId);
}
