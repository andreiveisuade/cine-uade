package ar.uade.cine.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasVentas;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.PedidoAccesoDTO;
import ar.uade.cine.dto.ventas.PedidoBloqueoDTO;
import ar.uade.cine.dto.ventas.PedidoReservaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.CriteriosReserva;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;

/**
 * Reservar, consultar y cancelar. El cliente no inicia sesión: se identifica con su
 * email, y si es la primera vez que compra se lo da de alta en el momento.
 *
 * <p>El bloqueo de butacas cuelga de la función porque son sus butacas las que se toman,
 * pero se atiende acá y no en {@link FuncionController}: es la primera etapa del
 * circuito de compra, no una operación sobre la programación. Un controlador agrupa por lo
 * que se pide, no por el prefijo de la URL.
 */
@RestController
public class ReservaController {

    private final GestorReservas reservas;
    private final GestorClientes clientes;
    private final Ocupacion ocupacion;
    private final VistasVentas vistas;

    public ReservaController(GestorReservas reservas, GestorClientes clientes,
                               Ocupacion ocupacion, VistasVentas vistas) {
        this.reservas = reservas;
        this.clientes = clientes;
        this.ocupacion = ocupacion;
        this.vistas = vistas;
    }

    /**
     * Sin email es el listado del encargado; con email, las reservas de ese cliente.
     *
     * <p>Que el criterio viaje en la URL y no se resuelva en la pantalla tiene dos
     * consecuencias que se pagan solas: el filtro se puede probar con un curl, y una
     * búsqueda se puede compartir pegando el link.
     */
    @GetMapping("/api/reservas")
    public List<ReservaVistaDTO> listar(@RequestParam(required = false) String email,
                                        @RequestParam(required = false) String estado,
                                        @RequestParam(required = false) String dia,
                                        @RequestParam(required = false) String q) {
        List<Reserva> lista = email == null || email.isBlank()
                ? reservas.buscar(new CriteriosReserva(
                        Parseo.constanteOpcional(EstadoReserva.class, estado, "el estado"),
                        Parseo.diaOpcional(dia, "el día"), q))
                // `email` es aparte: es el buscador del cliente en la web pública, que pide
                // la coincidencia exacta y no la parcial de `q`.
                : clientes.buscarPorEmail(email.trim())
                        .map(c -> reservas.listarPorCliente(c.getId()))
                        // Que el email no exista y que no tenga reservas son lo mismo para
                        // quien pregunta: lista vacía, no un 404.
                        .orElse(List.of());
        // vistas.reservas() y no un map de vistas.reserva(): el segundo pide la funcion, la
        // sala, la pelicula, el cliente y el pago de cada fila por separado, o sea cinco
        // consultas por reserva listada.
        return vistas.reservas(lista.stream()
                .sorted(Comparator.comparing(Reserva::getId).reversed())
                .toList());
    }

    @GetMapping("/api/reservas/{id}")
    public ReservaVistaDTO detalle(@PathVariable int id) {
        return vistas.reserva(buscar(id));
    }

    @PostMapping("/api/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaVistaDTO reservar(@RequestBody PedidoReservaDTO pedido) {
        // Que al cliente nuevo se lo dé de alta acá mismo es la regla de comprar sin
        // registrarse, y vive en el gestor: esta capa solo pasa lo que llegó.
        Cliente cliente = clientes.identificar(pedido.nombre(), pedido.email());

        return vistas.reserva(reservas.reservar(
                pedido.funcionId() == null ? 0 : pedido.funcionId(),
                cliente.getId(),
                butacasPedidas(pedido),
                pedido.sesion()));
    }

    /**
     * La etapa de antes de la reserva: mientras alguien elige, sus butacas dejan de aparecer
     * libres para el resto. Vence solo, así que cerrar la pestaña las devuelve a la venta sin
     * que nadie avise.
     *
     * <p>Es POST y no PUT aunque sea idempotente porque no crea ni reemplaza un recurso con
     * URL propia: no hay un {@code GET /api/funciones/1/bloqueos/xxx} que devuelva esto. Se
     * manda la selección entera —y {@code butacas: []} para soltar todo— para que una sola
     * llamada por click alcance para tomar, renovar y soltar.
     */
    @PostMapping("/api/funciones/{id}/bloqueos")
    public BloqueoVistaDTO bloquear(@PathVariable int id, @RequestBody PedidoBloqueoDTO pedido) {
        List<String> pedidas = pedido.butacas() == null ? List.of() : pedido.butacas();
        List<String> conseguidas = ocupacion.bloquear(id, pedidas, pedido.sesion());
        // Las que se escaparon van aparte y no como error: las otras sí se consiguieron.
        List<String> rechazadas = pedidas.stream()
                .map(codigo -> codigo.trim().toUpperCase())
                .filter(codigo -> !conseguidas.contains(codigo))
                .toList();
        return new BloqueoVistaDTO(pedido.sesion(), conseguidas, rechazadas,
                Ocupacion.MIENTRAS_ELIGE.toSeconds());
    }

    /**
     * CU-18: lo que llama el acomodador al escanear el QR. Va por código y no por id porque
     * el código es lo que trae el QR y, como el cliente no inicia sesión, es la única
     * credencial: con el id se entraría probando números.
     *
     * <p>Es POST y no GET porque no es una consulta: marca la entrada como usada, y
     * repetirlo falla a propósito (R18).
     */
    @PostMapping("/api/acceso")
    public ReservaVistaDTO registrarIngreso(@RequestBody PedidoAccesoDTO pedido) {
        return vistas.reserva(reservas.registrarIngreso(pedido.codigo()));
    }

    /** R6: cancelar libera las butacas, y el cupo de la función deja de contarlas. */
    @PostMapping("/api/reservas/{id}/cancelacion")
    public ReservaVistaDTO cancelar(@PathVariable int id) {
        buscar(id);
        reservas.cancelar(id);
        return vistas.reserva(buscar(id));
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

    private Reserva buscar(int id) {
        return reservas.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la reserva " + id));
    }
}
