package ar.uade.cine.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.CompraCandy;

public interface CompraCandyRepository extends JpaRepository<CompraCandy, Integer> {

    List<CompraCandy> findByClienteId(int clienteId);

    boolean existsByClienteId(int clienteId);

    List<CompraCandy> findByReservaIdIn(Collection<Integer> reservaIds);

    @Query("select c from CompraCandy c where c.fecha >= :desde and c.fecha < :hasta order by c.fecha")
    List<CompraCandy> findEntre(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    default List<CompraCandy> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
