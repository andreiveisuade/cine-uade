package ar.uade.cine.repository.salas;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Sala;

public interface SalaRepository extends JpaRepository<Sala, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);
}
