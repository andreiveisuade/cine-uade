package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.promociones.Promocion;

/**
 * Las promociones, las tres clases en la misma tabla. Devuelve la subclase que corresponde
 * según el discriminador, así que quien pide una promoción recibe la que sabe calcular su
 * descuento sin preguntar de qué tipo es.
 */
public interface PromocionRepository extends JpaRepository<Promocion, Integer> {

    /** Las candidatas de un cobro: una promoción dada de baja no descuenta más. */
    List<Promocion> findByActivaTrue();
}
