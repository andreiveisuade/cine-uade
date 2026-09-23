package ar.uade.cine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.ventas.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    Optional<Reserva> findByCodigo(String codigo);

    List<Reserva> findByFuncion_Id(int funcionId);

    List<Reserva> findByCliente_IdOrderByCreadaEnDesc(int clienteId);

    boolean existsByFuncion_Id(int funcionId);

    boolean existsByCliente_Id(int clienteId);
}
