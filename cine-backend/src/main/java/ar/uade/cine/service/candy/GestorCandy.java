package ar.uade.cine.service.candy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;

@Service
@Transactional
public class GestorCandy {

    private final CompraCandyRepository compraCandyRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final GeneradorTicketCandy generadorTicket;
    private final GestorProductos productos;
    private final Reloj reloj;

    public GestorCandy(CompraCandyRepository compraCandyRepository, ClienteRepository clienteRepository, ReservaRepository reservaRepository,
                       GeneradorTicketCandy generadorTicket, GestorProductos productos, Reloj reloj) {
        this.compraCandyRepository = compraCandyRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.generadorTicket = generadorTicket;
        this.productos = productos;
        this.reloj = reloj;
    }

    public CompraCandy vender(Integer clienteId, Map<Integer, Integer> cantidades,
                              MedioPago medio, String codigoAutorizacion) {
        return vender(clienteId, null, cantidades, medio, codigoAutorizacion);
    }

    public CompraCandy venderParaReserva(int reservaId, Map<Integer, Integer> cantidades,
                                         MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + reservaId));
        return vender(reserva.getClienteId(), reservaId, cantidades, medio, codigoAutorizacion);
    }

    private CompraCandy vender(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                               MedioPago medio, String codigoAutorizacion) {
        Cliente cliente = clienteId == null ? null : clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (cantidades == null || cantidades.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos un producto");
        }
        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        String autorizacion = medio.autorizacion(codigoAutorizacion);

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
            items.add(new ItemCompra(producto, cantidad, producto.getPrecio()));
        }

        CompraCandy compra = new CompraCandy(clienteId, reservaId, reloj.ahora(), medio, autorizacion, items);
        compraCandyRepository.save(compra);
        generadorTicket.emitir(compra, cliente, compra.getAhorro());
        return compra;
    }

    @Transactional(readOnly = true)
    public List<CompraCandy> listarComprasDelDia(LocalDate fecha) {
        return compraCandyRepository.findByDia(fecha);
    }

    @Transactional(readOnly = true)
    public List<CompraCandy> listarComprasDe(int clienteId) {
        return compraCandyRepository.findByClienteId(clienteId);
    }
}
