package ar.uade.cine.repository.salas;


import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.Repositorio;

// Persistencia de salas; Repository de Spring Data, con la consulta del nombre repetido.
public interface SalaRepository extends Repositorio<Sala> {

    boolean existsByNombreIgnoreCase(String nombre);
}
