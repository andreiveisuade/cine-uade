package ar.uade.cine.repository.salas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Asiento;

// Persistencia de butacas; Repository de Spring Data, las de una sala por fila y número.
public interface AsientoRepository extends JpaRepository<Asiento, Integer> {

    List<Asiento> findBySala_IdOrderByFilaAscNumeroAsc(int salaId);
}
