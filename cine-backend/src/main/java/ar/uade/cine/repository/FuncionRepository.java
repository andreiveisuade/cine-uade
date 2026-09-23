package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.funciones.Funcion;

/** Los {@code exists} son para R12, que solo necesita saber si hay alguna. */
public interface FuncionRepository extends JpaRepository<Funcion, Integer> {

    List<Funcion> findByPelicula_Id(int peliculaId);

    List<Funcion> findBySala_Id(int salaId);

    List<Funcion> findByProgramacionId(int programacionId);

    boolean existsByPelicula_Id(int peliculaId);

    boolean existsBySala_Id(int salaId);
}
