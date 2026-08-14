package ar.uade.cine.api;

import java.time.LocalDate;
import java.util.List;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dto.candy.ArqueoCandyVistaDTO;
import ar.uade.cine.dto.candy.PedidoComboDTO;
import ar.uade.cine.dto.candy.PedidoDisponibilidadDTO;
import ar.uade.cine.dto.candy.PedidoProductoDTO;
import ar.uade.cine.dto.candy.PedidoVentaDTO;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorProductos;
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

    static void registrar(Javalin app, GestorCandy candy, GestorProductos carta, VistasCandy vistas) {

        // La carta que ve el cliente: solo lo que está a la venta.
        app.get("/api/candy/productos", ctx -> {
            boolean todos = "true".equalsIgnoreCase(ctx.queryParam("todos"));
            List<Producto> productos = todos ? carta.listar() : carta.listarDisponibles();
            ctx.json(productos.stream().map(vistas::producto).toList());
        });

        app.get("/api/candy/productos/{id}", ctx ->
                ctx.json(vistas.producto(buscar(carta, Parseo.id(ctx)))));

        app.post("/api/candy/productos", ctx -> {
            PedidoProductoDTO pedido = ctx.bodyAsClass(PedidoProductoDTO.class);
            Producto producto = carta.agregar(pedido.nombre(),
                    pedido.tipo() == null
                            ? null : Parseo.constante(TipoProducto.class, pedido.tipo(), "el tipo de producto"),
                    pedido.precio() == null ? 0 : pedido.precio());
            ctx.status(HttpStatus.CREATED).json(vistas.producto(producto));
        });

        // R14: el combo tiene que salir menos que sus componentes sueltos, y eso lo valida
        // el gestor contra la lista de precios.
        app.post("/api/candy/combos", ctx -> {
            PedidoComboDTO pedido = ctx.bodyAsClass(PedidoComboDTO.class);
            Producto combo = carta.armarCombo(pedido.nombre(),
                    pedido.precio() == null ? 0 : pedido.precio(), pedido.componentes());
            ctx.status(HttpStatus.CREATED).json(vistas.producto(combo));
        });

        // No hay DELETE: un producto puede estar en compras viejas, y borrarlo dejaría
        // esos tickets apuntando a la nada. Se saca de la carta y se repone.
        app.put("/api/candy/productos/{id}/disponibilidad", ctx -> {
            int id = Parseo.id(ctx);
            buscar(carta, id);
            Boolean disponible = ctx.bodyAsClass(PedidoDisponibilidadDTO.class).disponible();
            if (disponible == null) {
                throw new IllegalArgumentException("Falta decir si el producto queda disponible");
            }
            carta.cambiarDisponibilidad(id, disponible);
            ctx.json(vistas.producto(buscar(carta, id)));
        });

        app.post("/api/candy/compras", ctx -> {
            PedidoVentaDTO pedido = ctx.bodyAsClass(PedidoVentaDTO.class);
            MedioPago medio = pedido.medio() == null
                    ? null : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

            // Con reserva, el cliente sale de ella: es el «¿desea agregar pochoclos?» de
            // después de comprar la entrada, y no se lo vuelve a pedir.
            CompraCandy compra = pedido.reservaId() == null
                    ? candy.vender(pedido.clienteId(), pedido.cantidades(), medio, pedido.codigoAutorizacion())
                    : candy.venderParaReserva(pedido.reservaId(), pedido.cantidades(), medio,
                            pedido.codigoAutorizacion());

            ctx.status(HttpStatus.CREATED).json(vistas.compra(compra, carta.ahorroDe(compra)));
        });

        // El arqueo del candy es la otra caja del cine, aparte de la boletería.
        app.get("/api/candy/compras", ctx -> {
            String fecha = ctx.queryParam("fecha");
            String email = ctx.queryParam("clienteId");
            List<CompraCandy> compras = email != null && !email.isBlank()
                    ? candy.listarComprasDe(Integer.parseInt(email.trim()))
                    : candy.listarComprasDelDia(Parseo.dia(fecha, "la fecha"));
            ctx.json(compras.stream().map(c -> vistas.compra(c, carta.ahorroDe(c))).toList());
        });

        app.get("/api/candy/arqueo", ctx -> {
            LocalDate fecha = Parseo.dia(ctx.queryParam("fecha"), "la fecha");
            ctx.json(new ArqueoCandyVistaDTO(fecha.toString(), candy.totalVendido(fecha),
                    candy.listarComprasDelDia(fecha).stream()
                            .map(c -> vistas.compra(c, carta.ahorroDe(c)))
                            .toList()));
        });
    }

    private static Producto buscar(GestorProductos carta, int id) {
        return carta.buscar(id).orElseThrow(() -> new NoEncontrado("No existe el producto " + id));
    }
}
