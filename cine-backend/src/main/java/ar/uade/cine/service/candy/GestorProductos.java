package ar.uade.cine.service.candy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.ItemCombo;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.repository.ProductoRepository;

/**
 * La carta del candy: qué se vende y a qué precio.
 *
 * <p>Separada de {@link GestorCandy}, que registra las ventas, porque cambian por motivos
 * distintos: la carta cuando el cine suma un producto, la venta cuando cambia cómo se
 * cobra. Es la única fuente de precios del candy: la venta le pregunta cuánto sale cada
 * cosa en vez de aceptar el precio que le manden.
 */
@Service
@Transactional
public class GestorProductos {

    private final ProductoRepository productoRepository;

    public GestorProductos(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto agregar(String nombre, TipoProducto tipo, Dinero precio) {
        if (tipo == TipoProducto.COMBO) {
            throw new IllegalArgumentException("Un combo se arma con armarCombo, para que declare qué trae");
        }
        validarAlta(nombre, tipo, precio);
        Producto producto = new Producto(nombre.trim(), tipo, precio);
        productoRepository.save(producto);
        return producto;
    }

    /**
     * Arma la promoción: pochoclos + gaseosa a un precio menor que comprarlos por separado.
     *
     * <p>R14 es lo que hace que un combo sea una promoción y no un producto con nombre
     * bonito: si costara igual o más que sus componentes sueltos, no habría motivo para
     * ofrecerlo. Por eso se valida contra la lista de precios en vez de confiar en quien
     * lo carga.
     *
     * @param componentes id de producto a cantidad de unidades que trae el combo
     */
    public Producto armarCombo(String nombre, Dinero precio, Map<Integer, Integer> componentes) {
        validarAlta(nombre, TipoProducto.COMBO, precio);
        if (componentes == null || componentes.size() < 2) {
            throw new IllegalArgumentException("Un combo tiene que juntar al menos dos productos distintos");
        }

        Producto combo = new Producto(nombre.trim(), TipoProducto.COMBO, precio);
        for (Map.Entry<Integer, Integer> componente : componentes.entrySet()) {
            Producto producto = buscarOFallar(componente.getKey());
            int cantidad = componente.getValue();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de " + producto.getNombre()
                        + " en el combo debe ser mayor a cero");
            }
            if (producto.esCombo()) {
                throw new IllegalArgumentException("Un combo no puede contener otro combo: " + producto.getNombre());
            }
            combo.agregarComponente(new ItemCombo(producto, cantidad));
        }

        if (!combo.getPrecioSuelto().esMayorQue(precio)) {
            throw new IllegalArgumentException("El combo tiene que salir menos que sus componentes sueltos ($ "
                    + combo.getPrecioSuelto() + ")");
        }
        productoRepository.save(combo);
        return combo;
    }

    /** La carta que ve el cliente. */
    public List<Producto> listarDisponibles() {
        return productoRepository.findByDisponibleTrue();
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Optional<Producto> buscar(int id) {
        return productoRepository.findById(id);
    }

    /** Sacar de la carta o reponer. No se borra: hay compras viejas que lo referencian. */
    public void cambiarDisponibilidad(int productoId, boolean disponible) {
        Producto producto = buscarOFallar(productoId);
        producto.setDisponible(disponible);
        productoRepository.save(producto);
    }

    /** Lo que necesita la venta: el producto o el error, nunca un Optional vacío. */
    public Producto buscarOFallar(int id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + id));
    }

    private void validarAlta(String nombre, TipoProducto tipo, Dinero precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de producto");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (productoRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }
    }
}
