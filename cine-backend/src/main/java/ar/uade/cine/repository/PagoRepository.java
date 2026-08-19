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

/** Los cobros de las reservas. Es lo que lee el arqueo del día. */
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    /** Una reserva tiene a lo sumo un pago: el UNIQUE de la tabla lo garantiza. */
    Optional<Pago> findByReservaId(int reservaId);

    /**
     * Los pagos de varias reservas de una vez. Existe para que el listado de reservas no
     * pregunte el pago de cada fila por separado: cincuenta reservas eran cincuenta
     * consultas.
     */
    List<Pago> findByReservaIdIn(Collection<Integer> reservaIds);

    /**
     * Los cobros de un día. El corte se hace con un rango y no comparando la parte de fecha
     * de la columna porque una función de fecha en el WHERE deja el índice afuera —y porque
     * cómo se trunca una fecha lo escribe distinto cada motor.
     */
    @Query("select p from Pago p where p.fecha >= :desde and p.fecha < :hasta order by p.fecha")
    List<Pago> findEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    default List<Pago> findByDia(LocalDate dia) {
        return findEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }
}
