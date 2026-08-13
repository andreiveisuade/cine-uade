package ar.uade.cine.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.CriteriosReserva;
import ar.uade.cine.servicio.GestorReservas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Reservar, consultar y cancelar. El cliente no inicia sesión: se identifica con su
 * email, y si es la primera vez que compra se lo da de alta en el momento.
 */
class RutasReservas {

    /**
     * {@code butacas} es el pedido completo: código de butaca a tarifa de quien la ocupa.
     * {@code codigos} es la forma vieja, sin tarifas, y se sigue aceptando para no romper
     * a quien ya la use: se interpreta como todas GENERAL.
     */
    record PedidoReserva(Integer funcionId, String nombre, String email,
                         List<String> codigos, Map<String, TipoTarifa> butacas) {
    }

    /** El código del QR, que es lo único que tiene el acomodador en la puerta. */
    record PedidoAcceso(String codigo) {
    }

    static void registrar(Javalin app, GestorReservas reservas, GestorClientes clientes,
                          VistasVentas vistas) {

        // Sin email es el listado del encargado; con email, las reservas de ese cliente.
        // El listado, con los filtros del panel como query params. Que el criterio viaje
        // en la URL y no se resuelva en la pantalla tiene dos consecuencias que se pagan
        // solas: el filtro se puede probar con un curl, y una búsqueda se puede compartir
        // pegando el link.
        app.get("/api/reservas", ctx -> {
            String email = ctx.queryParam("email");
            List<Reserva> lista = email == null || email.isBlank()
                    ? reservas.buscar(new CriteriosReserva(
                            Parseo.constanteOpcional(EstadoReserva.class, ctx.queryParam("estado"), "el estado"),
                            Parseo.diaOpcional(ctx.queryParam("dia"), "el día"),
                            ctx.queryParam("q")))
                    // `email` es aparte: es el buscador del cliente en la web pública, que
                    // pide la coincidencia exacta y no la parcial de `q`.
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
            // Que al cliente nuevo se lo dé de alta acá mismo es la regla de comprar sin
            // registrarse, y vive en el gestor: esta capa solo pasa lo que llegó.
            Cliente cliente = clientes.identificar(pedido.nombre(), pedido.email());

            Reserva reserva = reservas.reservar(
                    pedido.funcionId() == null ? 0 : pedido.funcionId(),
                    cliente.getId(),
                    butacasPedidas(pedido));
            ctx.status(HttpStatus.CREATED).json(vistas.reserva(reserva));
        });

        /*
         * CU-18: lo que llama el acomodador al escanear el QR. Va por código y no por id
         * porque el código es lo que trae el QR y, como el cliente no inicia sesión, es
         * la única credencial: con el id se entraría probando números.
         *
         * Es POST y no GET porque no es una consulta: marca la entrada como usada, y
         * repetirlo falla a propósito (R18).
         */
        app.post("/api/acceso", ctx -> {
            PedidoAcceso pedido = ctx.bodyAsClass(PedidoAcceso.class);
            ctx.json(vistas.reserva(reservas.registrarIngreso(pedido.codigo())));
        });

        // R6: cancelar libera las butacas, y el cupo de la función deja de contarlas.
        app.post("/api/reservas/{id}/cancelacion", ctx -> {
            int id = Parseo.id(ctx);
            buscar(reservas, id);
            reservas.cancelar(id);
            ctx.json(vistas.reserva(buscar(reservas, id)));
        });
    }

    /** Sin tarifas explícitas, la lista vieja de códigos vale como todas GENERAL. */
    private static Map<String, TipoTarifa> butacasPedidas(PedidoReserva pedido) {
        if (pedido.butacas() != null && !pedido.butacas().isEmpty()) {
            return pedido.butacas();
        }
        if (pedido.codigos() == null) {
            return Map.of();
        }
        Map<String, TipoTarifa> generales = new LinkedHashMap<>();
        pedido.codigos().forEach(codigo -> generales.put(codigo, TipoTarifa.GENERAL));
        return generales;
    }

    private static Reserva buscar(GestorReservas reservas, int id) {
        return reservas.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la reserva " + id));
    }
}
