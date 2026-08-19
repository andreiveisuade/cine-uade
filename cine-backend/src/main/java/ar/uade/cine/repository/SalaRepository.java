package ar.uade.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Sala;

/** Las salas del cine. */
public interface SalaRepository extends JpaRepository<Sala, Integer> {
}
