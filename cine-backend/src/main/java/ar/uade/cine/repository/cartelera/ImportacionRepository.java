package ar.uade.cine.repository.cartelera;

import java.util.List;

import org.springframework.data.domain.Limit;

import ar.uade.cine.model.cartelera.Importacion;
import ar.uade.cine.repository.Repositorio;

// Persistencia de las corridas del importador; Repository de Spring Data, las últimas primero.
public interface ImportacionRepository extends Repositorio<Importacion> {

    List<Importacion> findAllByOrderByIdDesc(Limit cuantas);
}
