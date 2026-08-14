package ar.uade.cine.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.PedidoAccesoDTO;
import ar.uade.cine.dto.ventas.PedidoBloqueoDTO;
import ar.uade.cine.dto.ventas.PedidoReservaDTO;
import ar.uade.cine.servicio.CriteriosReserva;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.Ocupacion;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Reservar, consultar y cancelar. El cliente no inicia sesión: se identifica con su
 * email, y si es la primera vez que compra se lo da de alta en el momento.
 *
 * <p>El bloqueo de butacas cuelga de la función porque son sus butacas las que se toman,
 * pero se registra acá y no en {@link RutasFunciones}: es la primera etapa del circuito de
 * compra, no una operación sobre la programación.
 */
class RutasReservas {

    static void registrar(Javalin app, GestorReservas reservas, GestorClientes clientes,
                          Ocupacion ocupacion, VistasVentas vistas) {

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
            PedidoReservaDTO pedido = ctx.bodyAsClass(PedidoReservaDTO.class);
            // Que al cliente nuevo se lo dé de alta acá mismo es la regla de comprar sin
            // registrarse, y vive en el gestor: esta capa solo pasa lo que llegó.
            Cliente cliente = clientes.identificar(pedido.nombre(), pedido.email());

            Reserva reserva = reservas.reservar(
                    pedido.funcionId() == null ? 0 : pedido.funcionId(),
                    cliente.getId(),
                    butacasPedidas(pedido),
                    pedido.sesion());
            ctx.status(HttpStatus.CREATED).json(vistas.reserva(reserva));
        });

        /*
         * La etapa de antes de la reserva: mientras alguien elige, sus butacas dejan de
         * aparecer libres para el resto. Vence solo, así que cerrar la pestaña las devuelve
         * a la venta sin que nadie avise.
         *
         * Es POST y no PUT aunque sea idempotente porque no crea ni reemplaza un recurso
         * con URL propia: no hay un `GET /api/funciones/1/bloqueos/xxx` que devuelva esto.
         * Se manda la selección entera —y `butacas: []` para soltar todo— para que una sola
         * llamada por click alcance para tomar, renovar y soltar.
         */
        app.post("/api/funciones/{id}/bloqueos", ctx -> {
            PedidoBloqueoDTO pedido = ctx.bodyAsClass(PedidoBloqueoDTO.class);
            int funcionId = Parseo.id(ctx);
            List<String> pedidas = pedido.butacas() == null ? List.of() : pedido.butacas();
            List<String> conseguidas = ocupacion.bloquear(funcionId, pedidas, pedido.sesion());
            // Las que se escaparon van aparte y no como error: las otras sí se consiguieron.
            List<String> rechazadas = pedidas.stream()
                    .map(codigo -> codigo.trim().toUpperCase())
                    .filter(codigo -> !conseguidas.contains(codigo))
                    .toList();
            ctx.json(new BloqueoVistaDTO(pedido.sesion(), conseguidas, rechazadas,
                    Ocupacion.MIENTRAS_ELIGE.toSeconds()));
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
            PedidoAccesoDTO pedido = ctx.bodyAsClass(PedidoAccesoDTO.class);
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
    private static Map<String, TipoTarifa> butacasPedidas(PedidoReservaDTO pedido) {
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
