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
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.CriteriosReserva;
import ar.uade.cine.service.ventas.GestorAcceso;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reservas", description = "El circuito de compra: bloquear, reservar, entrar y cancelar")
@RestController
public class ReservaController {

    private final GestorReservas reservas;
    private final GestorAcceso acceso;
    private final ConsultasReservas consultas;
    private final GestorClientes clientes;
    private final Ocupacion ocupacion;
    private final VistasVentas vistas;

    public ReservaController(GestorReservas reservas, GestorAcceso acceso, ConsultasReservas consultas,
                             GestorClientes clientes, Ocupacion ocupacion, VistasVentas vistas) {
        this.reservas = reservas;
        this.acceso = acceso;
        this.consultas = consultas;
        this.clientes = clientes;
        this.ocupacion = ocupacion;
        this.vistas = vistas;
    }

    @Operation(summary = "Las reservas del cine; con email, las de ese cliente sin el código de acceso")
    @GetMapping("/api/reservas")
    public List<ReservaVistaDTO> listar(@RequestParam(required = false) String email,
                                        @RequestParam(required = false) String estado,
                                        @RequestParam(required = false) String dia,
                                        @RequestParam(required = false) String q) {
        if (email != null && !email.isBlank()) {
            // Sin código: la ruta es pública y el email no prueba ser el dueño.
            return vistas.reservasSinCodigo(ordenadas(clientes.buscarPorEmail(email.trim())
                    .map(c -> consultas.listarPorCliente(c.getId()))
                    .orElse(List.of())));
        }
        // vistas.reservas() y no un map de vistas.reserva(): evita cinco consultas por fila.
        return vistas.reservas(ordenadas(consultas.buscar(new CriteriosReserva(
                Parseo.constanteOpcional(EstadoReserva.class, estado, "el estado"),
                Parseo.diaOpcional(dia, "el día"), q))));
    }

    @Operation(summary = "El detalle de una reserva (encargado)")
    @GetMapping("/api/reservas/{id}")
    public ReservaVistaDTO detalle(@PathVariable int id) {
        return vistas.reserva(buscar(id));
    }

    @Operation(summary = "El ticket del cliente, por su código de acceso")
    @GetMapping("/api/reservas/codigo/{codigo}")
    public ReservaVistaDTO detallePorCodigo(@PathVariable String codigo) {
        return vistas.reserva(buscarPorCodigo(codigo));
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

    @Operation(summary = "Tomar butacas mientras el cliente elige. Vencen solas")
    @PostMapping("/api/funciones/{id}/bloqueos")
    public BloqueoVistaDTO bloquear(@PathVariable int id, @RequestBody PedidoBloqueoDTO pedido) {
        List<String> pedidas = pedido.butacas() == null ? List.of() : pedido.butacas();
        List<String> conseguidas = ocupacion.bloquear(id, pedidas, pedido.sesion());
        List<String> rechazadas = pedidas.stream()
                .map(Asiento::normalizarCodigo)
                .filter(codigo -> !conseguidas.contains(codigo))
                .toList();
        return new BloqueoVistaDTO(pedido.sesion(), conseguidas, rechazadas,
                Ocupacion.MIENTRAS_ELIGE.toSeconds());
    }

    // Por código y no por id: el código es la única credencial del cliente y el id se adivina.
    @Operation(summary = "Validar el QR en la puerta y marcar la entrada como usada")
    @PostMapping("/api/acceso")
    public ReservaVistaDTO registrarIngreso(@RequestBody PedidoAccesoDTO pedido) {
        return vistas.reserva(acceso.registrarIngreso(pedido.codigo()));
    }

    @Operation(summary = "Cancelar una reserva y liberar sus butacas (encargado)")
    @PostMapping("/api/reservas/{id}/cancelacion")
    public ReservaVistaDTO cancelar(@PathVariable int id) {
        buscar(id);
        reservas.cancelar(id);
        return vistas.reserva(buscar(id));
    }

    @Operation(summary = "El cliente cancela su reserva con el código de acceso")
    @PostMapping("/api/reservas/codigo/{codigo}/cancelacion")
    public ReservaVistaDTO cancelarPorCodigo(@PathVariable String codigo) {
        int id = buscarPorCodigo(codigo).getId();
        reservas.cancelar(id);
        return vistas.reserva(buscar(id));
    }

    private static List<Reserva> ordenadas(List<Reserva> lista) {
        return lista.stream().sorted(Comparator.comparing(Reserva::getId).reversed()).toList();
    }

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

    private Reserva buscarPorCodigo(String codigo) {
        return consultas.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoEncontrado("No existe ninguna reserva con ese código"));
    }

    private Reserva buscar(int id) {
        return consultas.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la reserva " + id));
    }
}
