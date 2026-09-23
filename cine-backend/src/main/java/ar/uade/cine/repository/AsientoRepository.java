package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Asiento;

public interface AsientoRepository extends JpaRepository<Asiento, Integer> {

    List<Asiento> findBySalaIdOrderByFilaAscNumeroAsc(int salaId);
}
