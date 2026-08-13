package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.persistencia.ProductoDAO;

/** Implementación en memoria: sirve para probar la lógica sin levantar MySQL. */
public class ProductoDAOMemoria implements ProductoDAO {

    private final Map<Integer, Producto> productos = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Producto producto) {
        producto.setId(proximoId++);
        productos.put(producto.getId(), producto);
    }

    @Override
    public void actualizar(Producto producto) {
        productos.put(producto.getId(), producto);
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        return Optional.ofNullable(productos.get(id));
    }

    @Override
    public List<Producto> listar() {
        return new ArrayList<>(productos.values());
    }

    @Override
    public List<Producto> listarDisponibles() {
        return productos.values().stream().filter(Producto::estaDisponible).toList();
    }
}
