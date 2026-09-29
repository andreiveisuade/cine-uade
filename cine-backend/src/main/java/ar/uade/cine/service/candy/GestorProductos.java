package ar.uade.cine.service.candy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.repository.candy.ProductoRepository;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

// Carta del candy y sus combos; Controlador: acá lo que pide la base, nombre libre y combos afectados.
// Nombre, precio, qué puede traer un combo y R14 los valida Producto al construirse o editarse.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorProductos {

    private final ProductoRepository productoRepository;

    // Se construye antes de buscar el nombre repetido para que un dato inválido se rechace primero.
    public Producto agregar(String nombre, TipoProducto tipo, Dinero precio) {
        Producto producto = new Producto(nombre, tipo, precio);
        exigirNombreLibre(producto.getNombre());
        return productoRepository.save(producto);
    }

    // El nombre repetido va antes de buscar los componentes: si el combo ya existe, no hace falta ir
    // a buscar lo que trae.
    public Producto armarCombo(String nombre, Dinero precio, Map<Integer, Integer> componentes) {
        exigirNombreLibre(nombre);
        return productoRepository.save(Producto.armarCombo(nombre, precio, buscarOFallar(componentes)));
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
        if (disponible) {
            producto.volverALaVenta();
        } else {
            producto.sacarDeLaVenta();
        }
        productoRepository.save(producto);
    }

    // El combo editado se revisa a sí mismo (R14); los combos que traen al suelto editado los busca
    // la base, y cada uno dice si sigue conviniendo.
    public Producto editar(int productoId, String nombre, Dinero precio) {
        Producto producto = buscarOFallar(productoId);
        // Antes de editar: con el producto ya modificado, la consulta haría flush y chocaría con el
        // UNIQUE del nombre.
        if (nombre != null && !producto.getNombre().equalsIgnoreCase(nombre.trim())) {
            exigirNombreLibre(nombre);
        }
        producto.editar(nombre, precio);
        productoRepository.findCombosQueTraen(productoId).forEach(Producto::exigirQueSigaConviniendo);
        return productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public Producto buscarOFallar(int id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + id));
    }

    // Un pedido id → cantidad, con cada producto ya buscado y en el orden en que llegó.
    @Transactional(readOnly = true)
    public Map<Producto, Integer> buscarOFallar(Map<Integer, Integer> cantidades) {
        Map<Producto, Integer> productos = new LinkedHashMap<>();
        if (cantidades != null) {
            cantidades.forEach((id, cantidad) -> productos.put(buscarOFallar(id), cantidad));
        }
        return productos;
    }

    // Como lo compara la base: sin mayúsculas ni espacios alrededor.
    private void exigirNombreLibre(String nombre) {
        if (nombre != null && productoRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new ConflictoDeNegocio("Ya existe un producto con ese nombre");
        }
    }
}
