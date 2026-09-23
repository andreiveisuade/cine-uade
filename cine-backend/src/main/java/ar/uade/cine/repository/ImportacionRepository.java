package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.Importacion;

public interface ImportacionRepository extends JpaRepository<Importacion, Integer> {

    List<Importacion> findAllByOrderByIdDesc(Limit cuantas);
}
