package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.candy.Producto;

/**
 * No tiene eliminar: un producto puede estar en compras ya emitidas, así que se saca de
 * circulación con estaDisponible en vez de borrarse. Es la misma lección que dejaron las
 * películas con funciones programadas.
 */
public interface ProductoDAO {

    void guardar(Producto producto);

    /** Para cambiar el precio o sacarlo de la carta. */
    void actualizar(Producto producto);

    Optional<Producto> buscarPorId(int id);

    List<Producto> listar();

    /** La carta que se le muestra al cliente. */
    List<Producto> listarDisponibles();
}
