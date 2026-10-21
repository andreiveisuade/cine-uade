package ar.uade.cine.repository.promociones;

import java.util.List;

import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.repository.Repositorio;

// Persistencia de promociones; Repository de Spring Data: las activas y el nombre repetido.
public interface PromocionRepository extends Repositorio<Promocion> {

    List<Promocion> findByActivaTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    // Sin flush: la promoción ya editada chocaría contra el UNIQUE con un 500 antes de este 409.
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, int id);
}
