package ar.uade.cine.repository.candy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.EstadoCompraCandy;
import ar.uade.cine.repository.Repositorio;

// Persistencia de las ventas del candy; Repository de Spring Data, con el corte por día de cobro del arqueo.
public interface CompraCandyRepository extends Repositorio<CompraCandy> {

    List<CompraCandy> findByClienteId(int clienteId);

    List<CompraCandy> findByClienteIdAndEstadoOrderByCobradaEnDesc(int clienteId, EstadoCompraCandy estado);

    List<CompraCandy> findByReservaIdIn(Collection<Integer> reservaIds);

    // R30: a lo sumo uno por reserva, porque pedir de nuevo reemplaza el anterior.
    Optional<CompraCandy> findFirstByReservaIdAndEstado(int reservaId, EstadoCompraCandy estado);

    // Por cobro y no por pedido: lo pendiente y lo anulado no tienen cobradaEn, así que no entran.
    @Query("select c from CompraCandy c where c.cobradaEn >= :desde and c.cobradaEn < :hasta order by c.cobradaEn")
    List<CompraCandy> findEntre(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    default List<CompraCandy> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
