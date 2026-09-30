package ar.uade.cine.service.candy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.repository.candy.ProductoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Carta del candy y sus combos; Controlador: acá lo que pide la base, nombre libre y combos afectados.
// Nombre, precio, qué puede traer un combo y R14 los validan los validadores que llama Producto.
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

    // Como el alta, nombre y precio se validan antes de buscar el repetido. El nombre repetido va antes de
    // buscar los componentes: si el combo ya existe, no hace falta ir a buscar lo que trae.
    public Producto armarCombo(String nombre, Dinero precio, Map<Integer, Integer> componentes) {
        exigirNombreLibre(Producto.validarNombreYPrecio(nombre, precio));
        return productoRepository.save(Producto.armarCombo(nombre, precio, obtener(componentes)));
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
    public Producto obtener(int id) {
        return productoRepository.exigir(id, "el producto");
    }

    // Un pedido id → cantidad, con cada producto ya buscado y en el orden en que llegó.
    @Transactional(readOnly = true)
    public Map<Producto, Integer> obtener(Map<Integer, Integer> cantidades) {
        Map<Producto, Integer> productos = new LinkedHashMap<>();
        if (cantidades != null) {
            cantidades.forEach((id, cantidad) -> productos.put(obtener(id), cantidad));
        }
        return productos;
    }

    public Producto sacarDeLaVenta(int productoId) {
        Producto producto = obtener(productoId);
        producto.sacarDeLaVenta();
        return producto;
    }

    public Producto volverALaVenta(int productoId) {
        Producto producto = obtener(productoId);
        producto.volverALaVenta();
        return producto;
    }

    // El combo editado se revisa a sí mismo (R14); los combos que traen al suelto editado los busca
    // la base, y cada uno dice si sigue conviniendo.
    public Producto editar(int productoId, String nombre, Dinero precio) {
        Producto producto = obtener(productoId);
        // Validado sin editar, como en el alta: un dato inválido se rechaza antes que el nombre repetido. Con
        // el producto ya modificado, la consulta haría flush y chocaría con el UNIQUE del nombre.
        // Excluyéndose por id, renombrarse a sí mismo nunca es repetido.
        String nombreValido = Producto.validarNombreYPrecio(nombre, precio);
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(nombreValido, productoId)) {
            throw nombreRepetido();
        }
        producto.editar(nombre, precio);
        productoRepository.findCombosQueTraen(productoId).forEach(Producto::exigirQueSigaConviniendo);
        return producto;
    }

    // Como lo compara la base: sin mayúsculas. Los espacios alrededor ya los sacó el validador.
    private void exigirNombreLibre(String nombreValido) {
        if (productoRepository.existsByNombreIgnoreCase(nombreValido)) {
            throw nombreRepetido();
        }
    }

    private static ConflictoDeNegocio nombreRepetido() {
        return new ConflictoDeNegocio("Ya existe un producto con ese nombre");
    }
}
