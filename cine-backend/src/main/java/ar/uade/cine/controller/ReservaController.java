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
import ar.uade.cine.model.salas.Asiento;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * El circuito de compra. El cliente no inicia sesión: se identifica con su email y se lo da
 * de alta en la primera compra. El bloqueo de butacas vive acá y no en
 * {@link FuncionController} porque es la primera etapa de la compra.
 */
@Tag(name = "Reservas", description = "El circuito de compra: bloquear, reservar, entrar y cancelar")
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

    /** Sin email es el listado del encargado; con email, las reservas de ese cliente. */
    @Operation(summary = "Las reservas del cine; con email, las de ese cliente")
    @GetMapping("/api/reservas")
    public List<ReservaVistaDTO> listar(@RequestParam(required = false) String email,
                                        @RequestParam(required = false) String estado,
                                        @RequestParam(required = false) String dia,
                                        @RequestParam(required = false) String q) {
        List<Reserva> lista = email == null || email.isBlank()
                ? reservas.buscar(new CriteriosReserva(
                        Parseo.constanteOpcional(EstadoReserva.class, estado, "el estado"),
                        Parseo.diaOpcional(dia, "el día"), q))
                // `email` pide coincidencia exacta, no la parcial de `q`.
                : clientes.buscarPorEmail(email.trim())
                        .map(c -> reservas.listarPorCliente(c.getId()))
                        // Email inexistente y sin reservas son lo mismo: lista vacía, no 404.
                        .orElse(List.of());
        // vistas.reservas() y no un map de vistas.reserva(): evita cinco consultas por fila.
        return vistas.reservas(lista.stream()
                .sorted(Comparator.comparing(Reserva::getId).reversed())
                .toList());
    }

    @Operation(summary = "El detalle de una reserva")
    @GetMapping("/api/reservas/{id}")
    public ReservaVistaDTO detalle(@PathVariable int id) {
        return vistas.reserva(buscar(id));
    }

    @Operation(summary = "Reservar butacas. Al cliente nuevo se lo da de alta en el momento")
    @PostMapping("/api/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaVistaDTO reservar(@RequestBody PedidoReservaDTO pedido) {
        return vistas.reserva(reservas.reservar(
                pedido.funcionId() == null ? 0 : pedido.funcionId(),
                pedido.nombre(), pedido.email(),
                butacasPedidas(pedido),
                pedido.sesion()));
    }

    /**
     * Mientras alguien elige, sus butacas dejan de aparecer libres; vence solo, así que cerrar
     * la pestaña las devuelve a la venta. Se manda la selección entera ({@code []} suelta todo)
     * para que una llamada por click alcance para tomar, renovar y soltar.
     */
    @Operation(summary = "Tomar butacas mientras el cliente elige. Vencen solas")
    @PostMapping("/api/funciones/{id}/bloqueos")
    public BloqueoVistaDTO bloquear(@PathVariable int id, @RequestBody PedidoBloqueoDTO pedido) {
        List<String> pedidas = pedido.butacas() == null ? List.of() : pedido.butacas();
        List<String> conseguidas = ocupacion.bloquear(id, pedidas, pedido.sesion());
        // Las que se escaparon van aparte y no como error: las otras sí se consiguieron.
        List<String> rechazadas = pedidas.stream()
                .map(Asiento::normalizarCodigo)
                .filter(codigo -> !conseguidas.contains(codigo))
                .toList();
        return new BloqueoVistaDTO(pedido.sesion(), conseguidas, rechazadas,
                Ocupacion.MIENTRAS_ELIGE.toSeconds());
    }

    /**
     * CU-18. Por código y no por id: el código es la única credencial del cliente, y con el
     * id se entraría probando números. POST porque marca la entrada como usada (R18).
     */
    @Operation(summary = "Validar el QR en la puerta y marcar la entrada como usada")
    @PostMapping("/api/acceso")
    public ReservaVistaDTO registrarIngreso(@RequestBody PedidoAccesoDTO pedido) {
        return vistas.reserva(reservas.registrarIngreso(pedido.codigo()));
    }

    /** R6: cancelar libera las butacas, y el cupo de la función deja de contarlas. */
    @Operation(summary = "Cancelar una reserva y liberar sus butacas")
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
