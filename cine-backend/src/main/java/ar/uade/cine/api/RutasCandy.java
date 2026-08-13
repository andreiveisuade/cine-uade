package ar.uade.cine.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.servicio.GestorCandy;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * La carta del candy y sus ventas por HTTP.
 *
 * <p>El candy funcionaba solo por consola: existía el gestor, con sus reglas y sus tests,
 * pero la puerta HTTP nunca lo expuso. No fue una decisión, fue la consecuencia de que
 * cada arranque armara la aplicación por su cuenta y uno se olvidara de ese gestor.
 *
 * <p>Son dos circuitos de venta distintos: acá no se reserva nada, se paga en el mostrador
 * y se entrega, así que la compra nace cobrada y no pasa por {@code /api/reservas}.
 */
class RutasCandy {

    /** Alta de un producto suelto. Un combo va por su propio endpoint. */
    record PedidoProducto(String nombre, String tipo, Double precio) {
    }

    /** {@code componentes} es id de producto a cuántas unidades trae el combo. */
    record PedidoCombo(String nombre, Double precio, Map<Integer, Integer> componentes) {
    }

    /**
     * {@code cantidades} es id de producto a unidades. {@code clienteId} y {@code reservaId}
     * son los dos opcionales: sin ninguno es una venta de mostrador, y con reserva el
     * cliente sale de ella.
     */
    record PedidoVenta(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                       String medio, String codigoAutorizacion) {
    }

    record PedidoDisponibilidad(Boolean disponible) {
    }

    static void registrar(Javalin app, GestorCandy candy, VistasCandy vistas) {

        // La carta que ve el cliente: solo lo que está a la venta.
        app.get("/api/candy/productos", ctx -> {
            boolean todos = "true".equalsIgnoreCase(ctx.queryParam("todos"));
            List<Producto> productos = todos ? candy.listar() : candy.listarDisponibles();
            ctx.json(productos.stream().map(vistas::producto).toList());
        });

        app.get("/api/candy/productos/{id}", ctx ->
                ctx.json(vistas.producto(buscar(candy, Parseo.id(ctx)))));

        app.post("/api/candy/productos", ctx -> {
            PedidoProducto pedido = ctx.bodyAsClass(PedidoProducto.class);
            Producto producto = candy.agregarProducto(pedido.nombre(),
                    pedido.tipo() == null
                            ? null : Parseo.constante(TipoProducto.class, pedido.tipo(), "el tipo de producto"),
                    pedido.precio() == null ? 0 : pedido.precio());
            ctx.status(HttpStatus.CREATED).json(vistas.producto(producto));
        });

        // R14: el combo tiene que salir menos que sus componentes sueltos, y eso lo valida
        // el gestor contra la lista de precios.
        app.post("/api/candy/combos", ctx -> {
            PedidoCombo pedido = ctx.bodyAsClass(PedidoCombo.class);
            Producto combo = candy.armarCombo(pedido.nombre(),
                    pedido.precio() == null ? 0 : pedido.precio(), pedido.componentes());
            ctx.status(HttpStatus.CREATED).json(vistas.producto(combo));
        });

        // No hay DELETE: un producto puede estar en compras viejas, y borrarlo dejaría
        // esos tickets apuntando a la nada. Se saca de la carta y se repone.
        app.put("/api/candy/productos/{id}/disponibilidad", ctx -> {
            int id = Parseo.id(ctx);
            buscar(candy, id);
            Boolean disponible = ctx.bodyAsClass(PedidoDisponibilidad.class).disponible();
            if (disponible == null) {
                throw new IllegalArgumentException("Falta decir si el producto queda disponible");
            }
            candy.cambiarDisponibilidad(id, disponible);
            ctx.json(vistas.producto(buscar(candy, id)));
        });

        app.post("/api/candy/compras", ctx -> {
            PedidoVenta pedido = ctx.bodyAsClass(PedidoVenta.class);
            MedioPago medio = pedido.medio() == null
                    ? null : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

            // Con reserva, el cliente sale de ella: es el «¿desea agregar pochoclos?» de
            // después de comprar la entrada, y no se lo vuelve a pedir.
            CompraCandy compra = pedido.reservaId() == null
                    ? candy.vender(pedido.clienteId(), pedido.cantidades(), medio, pedido.codigoAutorizacion())
                    : candy.venderParaReserva(pedido.reservaId(), pedido.cantidades(), medio,
                            pedido.codigoAutorizacion());

            ctx.status(HttpStatus.CREATED).json(vistas.compra(compra, candy.ahorroDe(compra)));
        });

        // El arqueo del candy es la otra caja del cine, aparte de la boletería.
        app.get("/api/candy/compras", ctx -> {
            String fecha = ctx.queryParam("fecha");
            String email = ctx.queryParam("clienteId");
            List<CompraCandy> compras = email != null && !email.isBlank()
                    ? candy.listarComprasDe(Integer.parseInt(email.trim()))
                    : candy.listarComprasDelDia(Parseo.dia(fecha, "la fecha"));
            ctx.json(compras.stream().map(c -> vistas.compra(c, candy.ahorroDe(c))).toList());
        });

        app.get("/api/candy/arqueo", ctx -> {
            LocalDate fecha = Parseo.dia(ctx.queryParam("fecha"), "la fecha");
            ctx.json(new ArqueoCandyVista(fecha.toString(), candy.totalVendido(fecha),
                    candy.listarComprasDelDia(fecha).stream()
                            .map(c -> vistas.compra(c, candy.ahorroDe(c)))
                            .toList()));
        });
    }

    record ArqueoCandyVista(String fecha, double total, List<VistasCandy.CompraCandyVista> compras) {
    }

    private static Producto buscar(GestorCandy candy, int id) {
        return candy.buscar(id).orElseThrow(() -> new NoEncontrado("No existe el producto " + id));
    }
}
