package ar.uade.cine.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.candy.ArqueoCandyVistaDTO;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.candy.PedidoComboDTO;
import ar.uade.cine.dto.candy.PedidoDisponibilidadDTO;
import ar.uade.cine.dto.candy.PedidoEdicionProductoDTO;
import ar.uade.cine.dto.candy.PedidoProductoDTO;
import ar.uade.cine.dto.candy.PedidoVentaDTO;
import ar.uade.cine.dto.candy.ProductoVistaDTO;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Candy", description = "La carta del candy y sus ventas de mostrador")
@RestController
public class CandyController {

    private final GestorCandy candy;
    private final GestorProductos carta;
    private final GestorCaja caja;
    private final VistasCandy vistas;

    public CandyController(GestorCandy candy, GestorProductos carta, GestorCaja caja,
                            VistasCandy vistas) {
        this.candy = candy;
        this.carta = carta;
        this.caja = caja;
        this.vistas = vistas;
    }

    @Operation(summary = "La carta del candy")
    @GetMapping("/api/candy/productos")
    public List<ProductoVistaDTO> productos(
            @RequestParam(required = false, defaultValue = "false") boolean todos) {
        List<Producto> productos = todos ? carta.listar() : carta.listarDisponibles();
        return productos.stream().map(vistas::producto).toList();
    }

    @Operation(summary = "El detalle de un producto")
    @GetMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO producto(@PathVariable int id) {
        return vistas.producto(buscar(id));
    }

    @Operation(summary = "Dar de alta un producto")
    @PostMapping("/api/candy/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoVistaDTO agregar(@Valid @RequestBody PedidoProductoDTO pedido) {
        Producto producto = carta.agregar(pedido.nombre(),
                Parseo.constante(TipoProducto.class, pedido.tipo(), "el tipo de producto"),
                Dinero.de(pedido.precio()));
        return vistas.producto(producto);
    }

    @Operation(summary = "Armar un combo con productos de la carta")
    @PostMapping("/api/candy/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoVistaDTO armarCombo(@Valid @RequestBody PedidoComboDTO pedido) {
        return vistas.producto(carta.armarCombo(pedido.nombre(), Dinero.de(pedido.precio()),
                pedido.componentes()));
    }

    @Operation(summary = "Editar nombre y precio de un producto o combo")
    @PutMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO editar(@PathVariable int id, @Valid @RequestBody PedidoEdicionProductoDTO pedido) {
        buscar(id);
        return vistas.producto(carta.editar(id, pedido.nombre(), Dinero.de(pedido.precio())));
    }

    @Operation(summary = "Sacar un producto de la carta, o reponerlo")
    @PutMapping("/api/candy/productos/{id}/disponibilidad")
    public ProductoVistaDTO cambiarDisponibilidad(@PathVariable int id,
                                                  @Valid @RequestBody PedidoDisponibilidadDTO pedido) {
        buscar(id);
        carta.cambiarDisponibilidad(id, pedido.disponible());
        return vistas.producto(buscar(id));
    }

    @Operation(summary = "Vender candy en el mostrador: nace cobrado")
    @PostMapping("/api/candy/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraCandyVistaDTO vender(@Valid @RequestBody PedidoVentaDTO pedido) {
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

        CompraCandy compra = pedido.reservaId() == null
                ? candy.vender(pedido.clienteId(), pedido.cantidades(), medio, pedido.codigoAutorizacion())
                : candy.venderParaReserva(pedido.reservaId(), pedido.cantidades(), medio,
                        pedido.codigoAutorizacion());

        return vistas.compra(compra);
    }

    @Operation(summary = "Las compras de candy de un día, o las de un cliente")
    @GetMapping("/api/candy/compras")
    public List<CompraCandyVistaDTO> compras(@RequestParam(required = false) String fecha,
                                             @RequestParam(required = false) String clienteId) {
        List<CompraCandy> compras = clienteId != null && !clienteId.isBlank()
                ? candy.listarComprasDe(Parseo.numeroOpcional(clienteId, "el cliente"))
                : candy.listarComprasDelDia(Parseo.dia(fecha, "la fecha"));
        return compras.stream().map(vistas::compra).toList();
    }

    @Operation(summary = "El arqueo del candy de un día")
    @GetMapping("/api/candy/arqueo")
    public ArqueoCandyVistaDTO arqueo(@RequestParam(required = false) String fecha) {
        LocalDate dia = Parseo.dia(fecha, "la fecha");
        return new ArqueoCandyVistaDTO(dia.toString(), caja.totalCandyDe(dia).aPesos(),
                candy.listarComprasDelDia(dia).stream().map(vistas::compra).toList());
    }

    private Producto buscar(int id) {
        return carta.buscar(id).orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + id));
    }
}
