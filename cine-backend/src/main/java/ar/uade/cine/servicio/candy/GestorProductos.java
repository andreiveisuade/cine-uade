package ar.uade.cine.servicio.candy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.ItemCombo;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.ProductoImpl;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.persistencia.ProductoDAO;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * La carta del candy: qué se vende y a qué precio.
 *
 * <p>Está separada de {@link GestorCandy}, que registra las ventas, porque las dos cosas
 * cambian por motivos distintos: la carta se toca cuando el cine suma un producto o
 * arma una promoción, y la venta cuando cambia cómo se cobra o qué dice el ticket.
 * Cuando vivían juntas, un cambio de precios y un cambio de facturación tocaban el mismo
 * archivo sin tener nada que ver entre sí.
 *
 * <p>Es además la <strong>única</strong> fuente de precios del candy: la venta le pregunta
 * cuánto sale cada cosa en vez de aceptar el precio que le manden, igual que el pago de
 * una reserva toma el monto de la reserva y no de quien cobra.
 */
@Service
public class GestorProductos {

    private final ProductoDAO productoDAO;

    public GestorProductos(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public Producto agregar(String nombre, TipoProducto tipo, Dinero precio) {
        if (tipo == TipoProducto.COMBO) {
            throw new IllegalArgumentException("Un combo se arma con armarCombo, para que declare qué trae");
        }
        validarAlta(nombre, tipo, precio);
        Producto producto = new ProductoImpl(nombre.trim(), tipo, precio);
        productoDAO.guardar(producto);
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

        Producto combo = new ProductoImpl(nombre.trim(), TipoProducto.COMBO, precio);
        Dinero suelto = Dinero.CERO;
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
            combo.agregarComponente(new ItemCombo(producto.getId(), producto.getNombre(), cantidad));
            suelto = suelto.mas(producto.getPrecio().por(cantidad));
        }

        if (!suelto.esMayorQue(precio)) {
            throw new IllegalArgumentException(
                    "El combo tiene que salir menos que sus componentes sueltos ($ " + suelto + ")");
        }
        productoDAO.guardar(combo);
        return combo;
    }

    /**
     * Cuánto se ahorró el cliente por llevar combos en lugar de los productos sueltos.
     *
     * <p>Vive con la carta y no con la venta porque el ahorro sale de comparar precios, y
     * los precios los sabe la carta: la compra guarda lo que se cobró, no lo que habría
     * costado de otra manera.
     */
    public Dinero ahorroDe(CompraCandy compra) {
        Dinero ahorro = Dinero.CERO;
        for (ItemCompra item : compra.getItems()) {
            Producto producto = buscarOFallar(item.productoId());
            if (producto.esCombo()) {
                ahorro = ahorro.mas(precioSuelto(producto).menos(producto.getPrecio())
                        .por(item.cantidad()));
            }
        }
        return ahorro;
    }

    private Dinero precioSuelto(Producto combo) {
        return Dinero.sumar(combo.getComponentes().stream()
                .map(c -> buscarOFallar(c.productoId()).getPrecio().por(c.cantidad()))
                .toList());
    }

    /** La carta que ve el cliente. */
    public List<Producto> listarDisponibles() {
        return productoDAO.listarDisponibles();
    }

    public List<Producto> listar() {
        return productoDAO.listar();
    }

    public Optional<Producto> buscar(int id) {
        return productoDAO.buscarPorId(id);
    }

    /** Sacar de la carta o reponer. No se borra: hay compras viejas que lo referencian. */
    public void cambiarDisponibilidad(int productoId, boolean disponible) {
        Producto producto = buscarOFallar(productoId);
        producto.setDisponible(disponible);
        productoDAO.actualizar(producto);
    }

    /** Lo que necesita la venta: el producto o el error, nunca un Optional vacío. */
    public Producto buscarOFallar(int id) {
        return productoDAO.buscarPorId(id)
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
        boolean repetido = productoDAO.listar().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre.trim()));
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }
    }
}
