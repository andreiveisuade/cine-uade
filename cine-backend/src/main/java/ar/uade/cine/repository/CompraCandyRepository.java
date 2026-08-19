package ar.uade.cine.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.CompraCandy;

/** Las ventas del candy, con sus líneas. La otra caja del cine, aparte de la boletería. */
public interface CompraCandyRepository extends JpaRepository<CompraCandy, Integer> {

    List<CompraCandy> findByClienteId(int clienteId);

    List<CompraCandy> findByReservaId(int reservaId);

    /** Por rango y no por la parte de fecha de la columna, igual que los pagos. */
    @Query("select c from CompraCandy c where c.fecha >= :desde and c.fecha < :hasta order by c.fecha")
    List<CompraCandy> findEntre(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    default List<CompraCandy> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
