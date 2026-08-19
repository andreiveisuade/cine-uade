package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.candy.Producto;

/** La carta del candy. Los combos son productos: están en esta misma tabla. */
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /** Lo que se le muestra al cliente: un producto sin stock se saca de la carta, no se borra. */
    List<Producto> findByDisponibleTrue();
}
