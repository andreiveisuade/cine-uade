package ar.uade.cine.repository.promociones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.promociones.Promocion;

// Persistencia de promociones; Repository de Spring Data: las activas al cobrar y el nombre repetido.
public interface PromocionRepository extends JpaRepository<Promocion, Integer> {

    List<Promocion> findByActivaTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}
