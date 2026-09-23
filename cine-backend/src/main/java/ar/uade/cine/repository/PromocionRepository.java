package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.promociones.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Integer> {

    List<Promocion> findByActivaTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}
