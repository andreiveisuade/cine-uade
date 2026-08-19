package ar.uade.cine.api.controladores;

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

import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.api.vistas.VistasCandy;
import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dto.candy.ArqueoCandyVistaDTO;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.candy.PedidoComboDTO;
import ar.uade.cine.dto.candy.PedidoDisponibilidadDTO;
import ar.uade.cine.dto.candy.PedidoProductoDTO;
import ar.uade.cine.dto.candy.PedidoVentaDTO;
import ar.uade.cine.dto.candy.ProductoVistaDTO;
import ar.uade.cine.servicio.candy.GestorCandy;
import ar.uade.cine.servicio.candy.GestorProductos;
import ar.uade.cine.servicio.informes.GestorCaja;

/**
 * La carta del candy y sus ventas por HTTP.
 *
 * <p>El candy llegó a existir sin estar publicado: existía el gestor, con sus reglas y sus
 * tests, pero la puerta HTTP nunca lo expuso. No fue una decisión, fue la consecuencia de
 * que cada arranque armara la aplicación por su cuenta y uno se olvidara de ese gestor. Con
 * el contenedor eso ya no puede pasar por olvido: un controlador anotado se descubre solo.
 *
 * <p>Son dos circuitos de venta distintos: acá no se reserva nada, se paga en el mostrador
 * y se entrega, así que la compra nace cobrada y no pasa por {@code /api/reservas}.
 */
@RestController
public class ControladorCandy {

    private final GestorCandy candy;
    private final GestorProductos carta;
    private final GestorCaja caja;
    private final VistasCandy vistas;

    public ControladorCandy(GestorCandy candy, GestorProductos carta, GestorCaja caja,
                            VistasCandy vistas) {
        this.candy = candy;
        this.carta = carta;
        this.caja = caja;
        this.vistas = vistas;
    }

    /** La carta que ve el cliente: solo lo que está a la venta. */
    @GetMapping("/api/candy/productos")
    public List<ProductoVistaDTO> productos(
            @RequestParam(required = false, defaultValue = "false") boolean todos) {
        List<Producto> productos = todos ? carta.listar() : carta.listarDisponibles();
        return productos.stream().map(vistas::producto).toList();
    }

    @GetMapping("/api/candy/productos/{id}")
    public ProductoVistaDTO producto(@PathVariable int id) {
        return vistas.producto(buscar(id));
    }

    @PostMapping("/api/candy/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoVistaDTO agregar(@RequestBody PedidoProductoDTO pedido) {
        Producto producto = carta.agregar(pedido.nombre(),
                pedido.tipo() == null
                        ? null : Parseo.constante(TipoProducto.class, pedido.tipo(), "el tipo de producto"),
                Dinero.de(pedido.precio() == null ? 0 : pedido.precio()));
        return vistas.producto(producto);
    }

    /**
     * R14: el combo tiene que salir menos que sus componentes sueltos, y eso lo valida el
     * gestor contra la lista de precios.
     */
    @PostMapping("/api/candy/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoVistaDTO armarCombo(@RequestBody PedidoComboDTO pedido) {
        return vistas.producto(carta.armarCombo(pedido.nombre(),
                Dinero.de(pedido.precio() == null ? 0 : pedido.precio()), pedido.componentes()));
    }

    /**
     * No hay DELETE: un producto puede estar en compras viejas, y borrarlo dejaría esos
     * tickets apuntando a la nada. Se saca de la carta y se repone.
     */
    @PutMapping("/api/candy/productos/{id}/disponibilidad")
    public ProductoVistaDTO cambiarDisponibilidad(@PathVariable int id,
                                                  @RequestBody PedidoDisponibilidadDTO pedido) {
        buscar(id);
        if (pedido.disponible() == null) {
            throw new IllegalArgumentException("Falta decir si el producto queda disponible");
        }
        carta.cambiarDisponibilidad(id, pedido.disponible());
        return vistas.producto(buscar(id));
    }

    @PostMapping("/api/candy/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraCandyVistaDTO vender(@RequestBody PedidoVentaDTO pedido) {
        MedioPago medio = pedido.medio() == null
                ? null : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

        // Con reserva, el cliente sale de ella: es el «¿desea agregar pochoclos?» de después
        // de comprar la entrada, y no se lo vuelve a pedir.
        CompraCandy compra = pedido.reservaId() == null
                ? candy.vender(pedido.clienteId(), pedido.cantidades(), medio, pedido.codigoAutorizacion())
                : candy.venderParaReserva(pedido.reservaId(), pedido.cantidades(), medio,
                        pedido.codigoAutorizacion());

        return vistas.compra(compra, carta.ahorroDe(compra));
    }

    /** El arqueo del candy es la otra caja del cine, aparte de la boletería. */
    @GetMapping("/api/candy/compras")
    public List<CompraCandyVistaDTO> compras(@RequestParam(required = false) String fecha,
                                             @RequestParam(required = false) String clienteId) {
        List<CompraCandy> compras = clienteId != null && !clienteId.isBlank()
                ? candy.listarComprasDe(Integer.parseInt(clienteId.trim()))
                : candy.listarComprasDelDia(Parseo.dia(fecha, "la fecha"));
        return compras.stream().map(c -> vistas.compra(c, carta.ahorroDe(c))).toList();
    }

    @GetMapping("/api/candy/arqueo")
    public ArqueoCandyVistaDTO arqueo(@RequestParam(required = false) String fecha) {
        LocalDate dia = Parseo.dia(fecha, "la fecha");
        return new ArqueoCandyVistaDTO(dia.toString(), caja.totalCandyDe(dia).aPesos(),
                candy.listarComprasDelDia(dia).stream()
                        .map(c -> vistas.compra(c, carta.ahorroDe(c)))
                        .toList());
    }

    private Producto buscar(int id) {
        return carta.buscar(id).orElseThrow(() -> new NoEncontrado("No existe el producto " + id));
    }
}
