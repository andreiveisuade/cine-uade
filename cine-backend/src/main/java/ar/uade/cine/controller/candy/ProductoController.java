package ar.uade.cine.controller.candy;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.dto.candy.PedidoComboDTO;
import ar.uade.cine.dto.candy.PedidoDisponibilidadDTO;
import ar.uade.cine.dto.candy.PedidoEdicionProductoDTO;
import ar.uade.cine.dto.candy.PedidoProductoDTO;
import ar.uade.cine.dto.candy.ProductoVistaDTO;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de la carta del candy (productos y combos): traduce HTTP a GestorProductos; no decide nada.
// Las ventas van en CompraCandyController y el arqueo en CajaController: mismo tag, otra responsabilidad.
@Tag(name = "Candy", description = "La carta del candy y sus ventas de mostrador")
@RestController
@RequiredArgsConstructor
public class ProductoController {

    private final GestorProductos carta;
    private final VistasCandy vistas;

    @Operation(summary = "La carta del candy")
    @GetMapping("/api/candy/productos")
    public List<ProductoVistaDTO> productos(@RequestParam(required = false) String todos) {
        List<Producto> productos = Boolean.TRUE.equals(Parseo.booleanOpcional(todos, "el filtro todos"))
                ? carta.listar() : carta.listarDisponibles();
        return productos.stream().map(vistas::producto).toList();
    }

    @Operation(summary = "El detalle de un producto")
    @GetMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO producto(@PathVariable int id) {
        return vistas.producto(carta.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + id)));
    }

    @Operation(summary = "Dar de alta un producto")
    @PostMapping("/api/candy/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductoVistaDTO> agregar(@Valid @RequestBody PedidoProductoDTO pedido) {
        Producto producto = carta.agregar(pedido.nombre(),
                Parseo.constante(TipoProducto.class, pedido.tipo(), "el tipo de producto"),
                Dinero.de(pedido.precio()));
        return creado(producto);
    }

    @Operation(summary = "Armar un combo con productos de la carta")
    @PostMapping("/api/candy/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductoVistaDTO> armarCombo(@Valid @RequestBody PedidoComboDTO pedido) {
        return creado(carta.armarCombo(pedido.nombre(), Dinero.de(pedido.precio()),
                pedido.componentes()));
    }

    @Operation(summary = "Editar nombre y precio de un producto o combo")
    @PutMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO editar(@PathVariable int id, @Valid @RequestBody PedidoEdicionProductoDTO pedido) {
        return vistas.producto(carta.editar(id, pedido.nombre(), Dinero.de(pedido.precio())));
    }

    @Operation(summary = "Sacar un producto de la carta, o reponerlo")
    @PatchMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO cambiarDisponibilidad(@PathVariable int id,
                                                  @Valid @RequestBody PedidoDisponibilidadDTO pedido) {
        Producto producto = pedido.disponible() ? carta.volverALaVenta(id) : carta.sacarDeLaVenta(id);
        return vistas.producto(producto);
    }

    private ResponseEntity<ProductoVistaDTO> creado(Producto producto) {
        return Creado.en("/api/candy/productos/" + producto.getId(), vistas.producto(producto));
    }
}
