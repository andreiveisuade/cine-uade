package ar.uade.cine.service.candy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.Combo;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.ProductoSuelto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.repository.candy.ProductoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Carta del candy y sus combos; Controlador: acá lo que pide la base, nombre libre y combos afectados.
// Nombre, precio, componentes y R14 los validan ProductoSuelto y Combo.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorProductos {

    private final ProductoRepository productoRepository;

    // Se construye antes del chequeo de nombre repetido: un dato inválido se rechaza primero
    public Producto agregar(String nombre, TipoProducto tipo, Dinero precio) {
        Producto producto = new ProductoSuelto(nombre, tipo, precio);
        exigirNombreLibre(producto.getNombre());
        return productoRepository.save(producto);
    }

    // Como el alta: se valida, luego el nombre repetido y recién después se buscan los componentes.
    public Combo armarCombo(String nombre, Dinero precio, Map<Integer, Integer> componentes) {
        exigirNombreLibre(Producto.validarNombreYPrecio(nombre, precio));
        return productoRepository.save(Combo.armar(nombre, precio, obtener(componentes)));
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

    // El combo se revisa a sí mismo (R14); los combos que traen al suelto editado los busca la base.
    public Producto editar(int productoId, String nombre, Dinero precio) {
        Producto producto = obtener(productoId);
        // Se valida sin editar: con el producto ya modificado, la consulta haría flush y chocaría con el UNIQUE.
        String nombreValido = Producto.validarNombreYPrecio(nombre, precio);
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(nombreValido, productoId)) {
            throw nombreRepetido();
        }
        producto.editar(nombre, precio);
        productoRepository.findCombosQueTraen(productoId).forEach(Combo::exigirQueSigaConviniendo);
        return producto;
    }

    // Sin mayúsculas, como lo compara la base
    private void exigirNombreLibre(String nombreValido) {
        if (productoRepository.existsByNombreIgnoreCase(nombreValido)) {
            throw nombreRepetido();
        }
    }

    private static ConflictoDeNegocio nombreRepetido() {
        return new ConflictoDeNegocio("Ya existe un producto con ese nombre");
    }
}
