package ar.uade.cine.repository.salas;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Sala;

// Persistencia de salas; Repository de Spring Data, con la consulta del nombre repetido.
public interface SalaRepository extends JpaRepository<Sala, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);
}
