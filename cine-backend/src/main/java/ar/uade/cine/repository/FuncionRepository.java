package ar.uade.cine.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.funciones.Funcion;

public interface FuncionRepository extends JpaRepository<Funcion, Integer> {

    List<Funcion> findByPelicula_Id(int peliculaId);

    List<Funcion> findBySala_Id(int salaId);

    List<Funcion> findByProgramacionId(int programacionId);

    boolean existsByPelicula_Id(int peliculaId);

    boolean existsBySala_Id(int salaId);

    @Query("""
            select f from Funcion f
            where (:peliculaId is null or f.pelicula.id = :peliculaId)
              and (:salaId is null or f.sala.id = :salaId)
              and (:desde is null or f.inicio >= :desde)
              and (:hasta is null or f.inicio < :hasta)
            order by f.id""")
    List<Funcion> buscar(@Param("peliculaId") Integer peliculaId, @Param("salaId") Integer salaId,
                         @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
