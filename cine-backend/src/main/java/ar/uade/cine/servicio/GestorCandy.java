package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.CompraCandyImpl;
import ar.uade.cine.dominio.candy.ItemCombo;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.ProductoImpl;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.GeneradorTicketCandy;
import ar.uade.cine.persistencia.ProductoDAO;

/**
 * La carta del candy y sus ventas. Es un circuito distinto al de las butacas: acá no se
 * reserva nada, se paga en el mostrador y se entrega, así que la compra nace cobrada.
 */
public class GestorCandy {

    private final ProductoDAO productoDAO;
    private final CompraCandyDAO compraDAO;
    private final ClienteDAO clienteDAO;
    private final GeneradorTicketCandy generadorTicket;

    public GestorCandy(ProductoDAO productoDAO, CompraCandyDAO compraDAO, ClienteDAO clienteDAO,
                       GeneradorTicketCandy generadorTicket) {
        this.productoDAO = productoDAO;
        this.compraDAO = compraDAO;
        this.clienteDAO = clienteDAO;
        this.generadorTicket = generadorTicket;
    }

    public Producto agregarProducto(String nombre, TipoProducto tipo, double precio) {
        if (tipo == TipoProducto.COMBO) {
            throw new IllegalArgumentException("Un combo se arma con armarCombo, para que declare qué trae");
        }
        validarAlta(nombre, tipo, precio);
        Producto producto = new ProductoImpl(nombre.trim(), tipo, precio);
        productoDAO.guardar(producto);
        return producto;
    }

    /**
     * Arma la promoción: pochoclos + gaseosa a un precio menor que comprarlos por separado.
     *
     * <p>R14 es lo que hace que un combo sea una promoción y no un producto con nombre
     * bonito: si costara igual o más que sus componentes sueltos, no habría motivo para
     * ofrecerlo. Por eso se valida contra la lista de precios en vez de confiar en quien
     * lo carga.
     *
     * @param componentes id de producto a cantidad de unidades que trae el combo
     */
    public Producto armarCombo(String nombre, double precio, Map<Integer, Integer> componentes) {
        validarAlta(nombre, TipoProducto.COMBO, precio);
        if (componentes == null || componentes.size() < 2) {
            throw new IllegalArgumentException("Un combo tiene que juntar al menos dos productos distintos");
        }

        Producto combo = new ProductoImpl(nombre.trim(), TipoProducto.COMBO, precio);
        double suelto = 0;
        for (Map.Entry<Integer, Integer> componente : componentes.entrySet()) {
            Producto producto = buscarOFallar(componente.getKey());
            int cantidad = componente.getValue();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de " + producto.getNombre()
                        + " en el combo debe ser mayor a cero");
            }
            if (producto.esCombo()) {
                throw new IllegalArgumentException("Un combo no puede contener otro combo: " + producto.getNombre());
            }
            combo.agregarComponente(new ItemCombo(producto.getId(), producto.getNombre(), cantidad));
            suelto += producto.getPrecio() * cantidad;
        }

        if (precio >= suelto) {
            throw new IllegalArgumentException(String.format(
                    "El combo tiene que salir menos que sus componentes sueltos ($ %.2f)", suelto));
        }
        productoDAO.guardar(combo);
        return combo;
    }

    /**
     * Registra la venta y emite el ticket. El total sale de la lista de precios, no se
     * ingresa: es la misma razón por la que el pago de una reserva toma el monto de la
     * reserva y no de quien cobra.
     *
     * @param cantidades id de producto a cuántas unidades lleva
     */
    public CompraCandy vender(int clienteId, Map<Integer, Integer> cantidades,
                              MedioPago medio, String codigoAutorizacion) {
        Cliente cliente = clienteDAO.buscarPorId(clienteId)
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
            Producto producto = buscarOFallar(pedido.getKey());
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

        CompraCandy compra = new CompraCandyImpl(clienteId, LocalDateTime.now(), medio,
                codigoAutorizacion == null ? "" : codigoAutorizacion.trim(), items);
        compraDAO.guardar(compra);
        generadorTicket.emitir(compra, cliente, ahorroDe(compra));
        return compra;
    }

    /** Cuánto se ahorró el cliente por llevar combos en lugar de los productos sueltos. */
    public double ahorroDe(CompraCandy compra) {
        double ahorro = 0;
        for (ItemCompra item : compra.getItems()) {
            Producto producto = buscarOFallar(item.productoId());
            if (producto.esCombo()) {
                ahorro += (precioSuelto(producto) - producto.getPrecio()) * item.cantidad();
            }
        }
        return CalculadoraPrecio.redondear(ahorro);
    }

    private double precioSuelto(Producto combo) {
        return combo.getComponentes().stream()
                .mapToDouble(c -> buscarOFallar(c.productoId()).getPrecio() * c.cantidad())
                .sum();
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

    /** La carta que ve el cliente. */
    public List<Producto> listarDisponibles() {
        return productoDAO.listarDisponibles();
    }

    public List<Producto> listar() {
        return productoDAO.listar();
    }

    public Optional<Producto> buscar(int id) {
        return productoDAO.buscarPorId(id);
    }

    /** Sacar de la carta o reponer. No se borra: hay compras viejas que lo referencian. */
    public void cambiarDisponibilidad(int productoId, boolean disponible) {
        Producto producto = buscarOFallar(productoId);
        producto.setDisponible(disponible);
        productoDAO.actualizar(producto);
    }

    private void validarAlta(String nombre, TipoProducto tipo, double precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de producto");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        boolean repetido = productoDAO.listar().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre.trim()));
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }
    }

    private Producto buscarOFallar(int id) {
        return productoDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + id));
    }
}
