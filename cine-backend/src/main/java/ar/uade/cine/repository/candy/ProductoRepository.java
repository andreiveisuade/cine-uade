package ar.uade.cine.repository.candy;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.Combo;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.repository.Repositorio;

// Persistencia de la carta del candy; Repository de Spring Data, con los combos que traen un suelto (R14).
public interface ProductoRepository extends Repositorio<Producto> {

    List<Producto> findByDisponibleTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    // Al renombrar: excluye por id; comparar en Java ignora la collation de la base ("Maní" vs "Mani").
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, int id);

    @Query("""
            select distinct c from Combo c join c.componentes i
            where i.producto.id = :productoId
            order by c.id""")
    List<Combo> findCombosQueTraen(@Param("productoId") int productoId);
}
