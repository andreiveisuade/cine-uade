package ar.uade.cine.repository.promociones;

import java.util.List;


import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.repository.Repositorio;

// Persistencia de promociones; Repository de Spring Data: las activas al cobrar y el nombre repetido.
public interface PromocionRepository extends Repositorio<Promocion> {

    List<Promocion> findByActivaTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}
