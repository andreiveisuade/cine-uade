package ar.uade.cine.repository.candy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.Producto;

// Persistencia de la carta del candy; Repository de Spring Data, con los combos que traen un suelto (R14).
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByDisponibleTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    @Query("""
            select distinct c from Producto c join c.componentes i
            where c.tipo = ar.uade.cine.model.candy.TipoProducto.COMBO and i.producto.id = :productoId
            order by c.id""")
    List<Producto> findCombosQueTraen(@Param("productoId") int productoId);
}
