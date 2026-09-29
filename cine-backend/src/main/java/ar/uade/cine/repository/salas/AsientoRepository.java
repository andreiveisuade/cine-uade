package ar.uade.cine.repository.salas;

import java.util.List;


import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.repository.Repositorio;

// Persistencia de butacas; Repository de Spring Data, las de una sala por fila y número.
public interface AsientoRepository extends Repositorio<Asiento> {

    List<Asiento> findBySala_IdOrderByFilaAscNumeroAsc(int salaId);
}
