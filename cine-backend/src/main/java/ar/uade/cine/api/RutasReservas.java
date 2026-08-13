package ar.uade.cine.api;

import java.util.Comparator;
import java.util.List;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorReservas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Reservar, consultar y cancelar. El cliente no inicia sesión: se identifica con su
 * email, y si es la primera vez que compra se lo da de alta en el momento.
 */
class RutasReservas {

    record PedidoReserva(Integer funcionId, String nombre, String email, List<String> codigos) {
    }

    static void registrar(Javalin app, GestorReservas reservas, GestorClientes clientes,
                          Vistas vistas) {

        // Sin email es el listado del encargado; con email, las reservas de ese cliente.
        app.get("/api/reservas", ctx -> {
            String email = ctx.queryParam("email");
            List<Reserva> lista = email == null || email.isBlank()
                    ? reservas.listar()
                    : clientes.buscarPorEmail(email.trim())
                            .map(c -> reservas.listarPorCliente(c.getId()))
                            // Que el email no exista y que no tenga reservas son lo mismo
                            // para quien pregunta: lista vacía, no un 404.
                            .orElse(List.of());
            ctx.json(lista.stream()
                    .sorted(Comparator.comparing(Reserva::getId).reversed())
                    .map(vistas::reserva)
                    .toList());
        });

        app.get("/api/reservas/{id}", ctx ->
                ctx.json(vistas.reserva(buscar(reservas, Parseo.id(ctx)))));

        app.post("/api/reservas", ctx -> {
            PedidoReserva pedido = ctx.bodyAsClass(PedidoReserva.class);
            String email = pedido.email() == null ? "" : pedido.email().trim();
            Cliente cliente = clientes.buscarPorEmail(email)
                    .orElseGet(() -> clientes.registrar(pedido.nombre(), pedido.email()));

            Reserva reserva = reservas.reservar(
                    pedido.funcionId() == null ? 0 : pedido.funcionId(),
                    cliente.getId(),
                    pedido.codigos());
            ctx.status(HttpStatus.CREATED).json(vistas.reserva(reserva));
        });

        // R6: cancelar libera las butacas, y el cupo de la función deja de contarlas.
        app.post("/api/reservas/{id}/cancelacion", ctx -> {
            int id = Parseo.id(ctx);
            buscar(reservas, id);
            reservas.cancelar(id);
            ctx.json(vistas.reserva(buscar(reservas, id)));
        });
    }

    private static Reserva buscar(GestorReservas reservas, int id) {
        return reservas.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la reserva " + id));
    }
}
