package ar.uade.cine.repository.cartelera;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.Importacion;

// Persistencia de las corridas del importador; Repository de Spring Data, las últimas primero.
public interface ImportacionRepository extends JpaRepository<Importacion, Integer> {

    List<Importacion> findAllByOrderByIdDesc(Limit cuantas);
}
