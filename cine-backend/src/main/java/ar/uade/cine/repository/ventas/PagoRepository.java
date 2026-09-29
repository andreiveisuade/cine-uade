package ar.uade.cine.repository.ventas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.repository.Repositorio;

// Persistencia de pagos; Repository de Spring Data, con el corte por día como rango para usar el índice.
public interface PagoRepository extends Repositorio<Pago> {

    Optional<Pago> findByReservaId(int reservaId);

    boolean existsByReservaId(int reservaId);

    List<Pago> findByReservaIdIn(Collection<Integer> reservaIds);

    // Por rango y no truncando la fecha: una función en el WHERE deja el índice afuera.
    @Query("select p from Pago p where p.fecha >= :desde and p.fecha < :hasta order by p.fecha")
    List<Pago> findEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    default List<Pago> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
