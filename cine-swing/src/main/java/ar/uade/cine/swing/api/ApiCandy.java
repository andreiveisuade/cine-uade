package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.PedidoCombo;
import ar.uade.cine.swing.api.dto.candy.PedidoProducto;
import ar.uade.cine.swing.api.dto.candy.PedidoVenta;
import ar.uade.cine.swing.api.dto.candy.Producto;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// La carta y las ventas del candy (CU-13 a CU-16); su arqueo va en ApiInformes, junto al de boletería.
@RequiredArgsConstructor
public final class ApiCandy {

    private final ClienteHttp http;

    public List<Producto> obtenerProductosCandy(boolean todos) {
        return http.lista("/candy/productos" + (todos ? "?todos=true" : ""), Producto.class);
    }

    public Producto crearProductoCandy(PedidoProducto producto) {
        return http.post("/candy/productos", producto, Producto.class);
    }

    public Producto armarComboCandy(PedidoCombo combo) {
        return http.post("/candy/combos", combo, Producto.class);
    }

    public Producto editarProductoCandy(int id, String nombre, Double precio) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombre", nombre);
        cuerpo.put("precio", precio);
        return http.put("/candy/productos/" + id, cuerpo, Producto.class);
    }

    public Producto cambiarDisponibilidadCandy(int id, boolean disponible) {
        return http.patch("/candy/productos/" + id, Map.of("disponible", disponible), Producto.class);
    }

    public CompraCandy venderCandy(PedidoVenta venta) {
        return http.post("/candy/compras", venta, CompraCandy.class);
    }

    public List<CompraCandy> obtenerComprasCandy(Map<String, String> filtros) {
        return http.lista("/candy/compras" + Parametros.consulta(filtros), CompraCandy.class);
    }
}
