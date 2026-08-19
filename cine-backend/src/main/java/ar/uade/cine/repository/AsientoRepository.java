package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.salas.Asiento;

/** Las butacas, que se piden siempre por sala: son el mapa que se dibuja en pantalla. */
public interface AsientoRepository extends JpaRepository<Asiento, Integer> {

    /** En el orden en que se sientan: fila A antes que la B, y dentro de la fila por número. */
    List<Asiento> findBySalaIdOrderByFilaAscNumeroAsc(int salaId);
}
