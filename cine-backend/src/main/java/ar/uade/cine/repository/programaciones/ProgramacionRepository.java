package ar.uade.cine.repository.programaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.programaciones.Programacion;

// Persistencia de programaciones; Repository de Spring Data, con los filtros opcionales resueltos en la base.
public interface ProgramacionRepository extends JpaRepository<Programacion, Integer> {

    List<Programacion> findByActivaTrue();

    boolean existsBySala_Id(int salaId);

    @Query("""
            select p from Programacion p
            where (:peliculaId is null or p.pelicula.id = :peliculaId)
              and (:salaId is null or p.sala.id = :salaId)
              and (:activa is null or p.activa = :activa)
            order by p.id""")
    List<Programacion> buscar(@Param("peliculaId") Integer peliculaId, @Param("salaId") Integer salaId,
                              @Param("activa") Boolean activa);
}
