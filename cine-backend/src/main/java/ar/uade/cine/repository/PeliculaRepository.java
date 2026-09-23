package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;

public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {

    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, int id);

    List<Pelicula> findByEstadoRevision(EstadoRevision estado);
}
