package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;

/**
 * La búsqueda con filtros opcionales no está acá: la resuelve {@code GestorCartelera} sobre
 * la lista, porque tres condiciones combinables serían ocho consultas.
 */
public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {

    /** R1. Al editar se excluye la propia. */
    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, int id);

    List<Pelicula> findByEstadoRevision(EstadoRevision estado);
}
