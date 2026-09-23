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
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

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

    @Transactional(readOnly = true)
    public List<Producto> listarDisponibles() {
        return productoRepository.findByDisponibleTrue();
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscar(int id) {
        return productoRepository.findById(id);
    }

    public void cambiarDisponibilidad(int productoId, boolean disponible) {
        Producto producto = buscarOFallar(productoId);
        producto.setDisponible(disponible);
        productoRepository.save(producto);
    }

    // R14 se vuelve a mirar de los dos lados: el combo editado y cada combo que trae el suelto editado.
    public Producto editar(int productoId, String nombre, Dinero precio) {
        Producto producto = buscarOFallar(productoId);
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (!producto.getNombre().equalsIgnoreCase(nombre.trim())
                && productoRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new ConflictoDeNegocio("Ya existe un producto con ese nombre");
        }

        producto.editar(nombre.trim(), precio);
        List<Producto> afectados = producto.esCombo() ? List.of(producto) : combosQueTraen(productoId);
        for (Producto combo : afectados) {
            if (!combo.getPrecioSuelto().esMayorQue(combo.getPrecio())) {
                throw new IllegalArgumentException("Con ese precio, el combo " + combo.getNombre()
                        + " dejaría de salir menos que sus componentes sueltos ($ "
                        + combo.getPrecioSuelto() + ")");
            }
        }
        return productoRepository.save(producto);
    }

    private List<Producto> combosQueTraen(int productoId) {
        return productoRepository.findAll().stream()
                .filter(Producto::esCombo)
                .filter(combo -> combo.getComponentes().stream()
                        .anyMatch(item -> item.producto().getId() == productoId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Producto buscarOFallar(int id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + id));
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
            throw new ConflictoDeNegocio("Ya existe un producto con ese nombre");
        }
    }
}
