package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.programaciones.Programacion;

public interface ProgramacionRepository extends JpaRepository<Programacion, Integer> {

    List<Programacion> findByActivaTrue();
}
