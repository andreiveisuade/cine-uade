package ar.uade.cine.repository.ventas;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    Optional<Reserva> findByCodigo(String codigo);

    List<Reserva> findByFuncion_Id(int funcionId);

    List<Reserva> findByCliente_IdOrderByCreadaEnDesc(int clienteId);

    boolean existsByFuncion_Id(int funcionId);

    boolean existsByCliente_Id(int clienteId);

    // Trae función, película y cliente en la misma consulta: el filtro por texto los lee a todos.
    @Query("""
            select r from Reserva r
              join fetch r.funcion f join fetch f.pelicula join fetch r.cliente
            where (:estado is null or r.estado = :estado)
              and (:desde is null or (f.inicio >= :desde and f.inicio < :hasta))
            order by r.id""")
    List<Reserva> buscar(@Param("estado") EstadoReserva estado, @Param("desde") LocalDateTime desde,
                         @Param("hasta") LocalDateTime hasta);

    // Lo que muestra una reserva, en una sola consulta: los listados la usan en vez de leer
    // función, película, sala y cliente por fila.
    @Query("""
            select r from Reserva r
              join fetch r.funcion f join fetch f.pelicula join fetch f.sala join fetch r.cliente
            where r.id in :ids""")
    List<Reserva> findConDetalle(@Param("ids") Collection<Integer> ids);

    // Solo las cobradas: la declaración jurada no lee las que retienen butaca sin haber vendido.
    @Query("""
            select r from Reserva r
              join fetch r.funcion f join fetch f.pelicula join fetch f.sala
            where f.inicio >= :desde and f.inicio < :hasta
              and exists (select p.id from Pago p where p.reservaId = r.id)
            order by f.inicio, f.id, r.id""")
    List<Reserva> findCobradasDeFuncionesEntre(@Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta);
}
