package ar.uade.cine.repository.candy;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.repository.Repositorio;

// Persistencia de la carta del candy; Repository de Spring Data, con los combos que traen un suelto (R14).
public interface ProductoRepository extends Repositorio<Producto> {

    List<Producto> findByDisponibleTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    // Al renombrar: el producto se excluye por id y no comparando nombres en Java, porque la base compara
    // con su collation (utf8mb4_0900_ai_ci ignora acentos) y Java no. "Maní" → "Mani" chocaba consigo mismo.
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, int id);

    @Query("""
            select distinct c from Producto c join c.componentes i
            where c.tipo = ar.uade.cine.model.candy.TipoProducto.COMBO and i.producto.id = :productoId
            order by c.id""")
    List<Producto> findCombosQueTraen(@Param("productoId") int productoId);
}
