package ar.uade.cine.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.ventas.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    /** A lo sumo uno: lo garantiza el UNIQUE de la tabla. */
    Optional<Pago> findByReservaId(int reservaId);

    boolean existsByReservaId(int reservaId);

    /** Para que el listado de reservas no haga una consulta por fila. */
    List<Pago> findByReservaIdIn(Collection<Integer> reservaIds);

    /** Por rango y no truncando la fecha: una función en el WHERE deja el índice afuera y varía por motor. */
    @Query("select p from Pago p where p.fecha >= :desde and p.fecha < :hasta order by p.fecha")
    List<Pago> findEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    default List<Pago> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
