package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.CompraCandyImpl;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Las ventas del candy. Es un circuito distinto al de las butacas: acá no se reserva
 * nada, se paga en el mostrador y se entrega, así que la compra nace cobrada.
 *
 * <p>Qué se vende y a qué precio no es asunto suyo: eso lo sabe {@link GestorProductos},
 * al que le pregunta. Esa separación es la que hace que cargar un producto nuevo y
 * cambiar cómo se cobra sean dos cambios en dos archivos distintos.
 */
public class GestorCandy {

    private final CompraCandyDAO compraDAO;
    private final ClienteDAO clienteDAO;
    private final ReservaDAO reservaDAO;
    private final GeneradorTicketCandy generadorTicket;
    private final GestorProductos productos;

    public GestorCandy(CompraCandyDAO compraDAO, ClienteDAO clienteDAO, ReservaDAO reservaDAO,
                       GeneradorTicketCandy generadorTicket, GestorProductos productos) {
        this.compraDAO = compraDAO;
        this.clienteDAO = clienteDAO;
        this.reservaDAO = reservaDAO;
        this.generadorTicket = generadorTicket;
        this.productos = productos;
    }

    /**
     * Registra la venta y emite el ticket. El total sale de la lista de precios, no se
     * ingresa: es la misma razón por la que el pago de una reserva toma el monto de la
     * reserva y no de quien cobra.
     *
     * @param cantidades id de producto a cuántas unidades lleva
     */
    public CompraCandy vender(Integer clienteId, Map<Integer, Integer> cantidades,
                              MedioPago medio, String codigoAutorizacion) {
        return vender(clienteId, null, cantidades, medio, codigoAutorizacion);
    }

    /**
     * El <em>«¿desea agregar pochoclos + gaseosa?»</em> de después de comprar la entrada
     * por la web. El cliente no se vuelve a pedir: sale de la reserva.
     */
    public CompraCandy venderParaReserva(int reservaId, Map<Integer, Integer> cantidades,
                                         MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaDAO.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + reservaId));
        return vender(reserva.getClienteId(), reservaId, cantidades, medio, codigoAutorizacion);
    }

    private CompraCandy vender(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                               MedioPago medio, String codigoAutorizacion) {
        // Sin cliente es una venta de mostrador: se cobra y se entrega, sin nombre.
        Cliente cliente = clienteId == null ? null : clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (cantidades == null || cantidades.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos un producto");
        }
        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        if (medio.requiereAutorizacion() && (codigoAutorizacion == null || codigoAutorizacion.isBlank())) {
            throw new IllegalArgumentException("El pago con " + medio + " necesita código de autorización");
        }

        List<ItemCompra> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> pedido : cantidades.entrySet()) {
            Producto producto = productos.buscarOFallar(pedido.getKey());
            int cantidad = pedido.getValue();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de " + producto.getNombre()
                        + " debe ser mayor a cero");
            }
            if (!producto.estaDisponible()) {
                throw new IllegalArgumentException(producto.getNombre() + " no está disponible");
            }
            items.add(new ItemCompra(producto.getId(), producto.getNombre(), cantidad, producto.getPrecio()));
        }

        CompraCandy compra = new CompraCandyImpl(clienteId, reservaId, LocalDateTime.now(), medio,
                codigoAutorizacion == null ? "" : codigoAutorizacion.trim(), items);
        compraDAO.guardar(compra);
        generadorTicket.emitir(compra, cliente, productos.ahorroDe(compra));
        return compra;
    }

    /** Arqueo: cuánto entró por el candy en el día, aparte de lo que entró por boletería. */
    public double totalVendido(LocalDate fecha) {
        return CalculadoraPrecio.redondear(
                compraDAO.listarPorFecha(fecha).stream().mapToDouble(CompraCandy::getTotal).sum());
    }

    public List<CompraCandy> listarComprasDelDia(LocalDate fecha) {
        return compraDAO.listarPorFecha(fecha);
    }

    public List<CompraCandy> listarComprasDe(int clienteId) {
        return compraDAO.listarPorCliente(clienteId);
    }
}
