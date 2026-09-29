package ar.uade.cine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.repository.candy.ProductoRepository;

// El default de la base lo hereda cualquier repositorio: se prueba con uno, contra la base de los tests.
class RepositorioTest extends PruebaDeIntegracion {

    @Autowired
    private ProductoRepository productos;

    @Test
    void exigirDevuelveLoQueExisteYRechazaConElNombreDeLoQueFalta() {
        Producto agua = productos.save(new Producto("Agua", TipoProducto.BEBIDA, Dinero.de(1500)));

        assertEquals("Agua", productos.exigir(agua.getId(), "el producto").getNombre());
        assertEquals("No existe el producto 999",
                assertThrows(RecursoNoEncontrado.class, () -> productos.exigir(999, "el producto")).getMessage());
    }
}
