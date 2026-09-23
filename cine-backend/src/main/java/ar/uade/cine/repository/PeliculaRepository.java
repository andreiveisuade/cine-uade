package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;

/**
 * El catálogo de películas.
 *
 * <p>Declara solo las preguntas que se contestan con una condición entera: la búsqueda
 * del encargado —título, género y publicada, todos opcionales— la resuelve
 * {@code GestorCartelera} sobre la lista, porque tres condiciones combinables serían
 * ocho consultas o una con tres OR que no se puede leer.
 */
public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {

    /** R1: el título es único sin distinguir mayúsculas. Al editar se excluye la propia. */
    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, int id);

    List<Pelicula> findByEstadoRevision(EstadoRevision estado);
}
