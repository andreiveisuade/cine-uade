package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.programaciones.Programacion;

public interface ProgramacionRepository extends JpaRepository<Programacion, Integer> {

    List<Programacion> findByActivaTrue();

    @Query("""
            select p from Programacion p
            where (:peliculaId is null or p.peliculaId = :peliculaId)
              and (:salaId is null or p.salaId = :salaId)
              and (:activa is null or p.activa = :activa)
            order by p.id""")
    List<Programacion> buscar(@Param("peliculaId") Integer peliculaId, @Param("salaId") Integer salaId,
                              @Param("activa") Boolean activa);
}
