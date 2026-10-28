package ar.uade.cine.controller.ventas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.candy.VistasCandy;
import ar.uade.cine.controller.cartelera.VistasCartelera;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.salas.VistasSalas;
import ar.uade.cine.controller.usuarios.VistasUsuarios;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.BloqueoDeButacas;
import ar.uade.cine.service.ventas.ButacasBloqueadas;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorPagos;

// Arma los JSON de reservas y del bloqueo de butacas; Assembler que compone Cartelera, Salas y Pagos.
// Un listado cuesta consultas fijas: reservas con detalle en una, pagos y candy en otras, butacas por sala;
// VistasVentasTest cuenta las sentencias
@Component
@RequiredArgsConstructor
public class VistasReservas {

    private final GestorPagos pagos;
    private final GestorCandy candy;
    private final GestorSalas salas;
    private final ConsultasReservas reservas;
    private final VistasCartelera vistasCartelera;
    private final VistasSalas vistasSalas;
    private final VistasUsuarios vistasUsuarios;
    private final VistasPagos vistasPagos;
    private final VistasCandy vistasCandy;
    private final Reloj reloj;

    public List<ReservaVistaDTO> reservas(List<Reserva> lista) {
        return armar(lista);
    }

    public ReservaVistaDTO reserva(Reserva r) {
        return armar(List.of(r), false).get(0);
    }

    // La del ticket público por código: el email del cliente sale enmascarado (R25).
    public ReservaVistaDTO reservaPublica(Reserva r) {
        return armar(List.of(r), true).get(0);
    }

    private List<ReservaVistaDTO> armar(List<Reserva> lista) {
        return armar(lista, false);
    }

    private List<ReservaVistaDTO> armar(List<Reserva> lista, boolean publica) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = lista.stream().map(Reserva::getId).toList();
        Map<Integer, Reserva> conDetalle = reservas.conDetalle(ids).stream()
                .collect(Collectors.toMap(Reserva::getId, Function.identity()));
        Map<Integer, Pago> porReserva = pagos.buscarPorReservas(ids).stream()
                .collect(Collectors.toMap(Pago::getReservaId, Function.identity()));
        Map<Integer, List<CompraCandyVistaDTO>> candyPorReserva = candy.comprasDeReservas(ids).stream()
                .collect(Collectors.groupingBy(CompraCandy::getReservaId,
                        Collectors.mapping(vistasCandy::compra, Collectors.toList())));
        Map<Integer, List<Asiento>> asientosPorSala = new HashMap<>();

        return lista.stream()
                .map(r -> dto(conDetalle.get(r.getId()), asientosPorSala, porReserva.get(r.getId()),
                        candyPorReserva.getOrDefault(r.getId(), List.of()), publica))
                .toList();
    }

    private ReservaVistaDTO dto(Reserva r, Map<Integer, List<Asiento>> asientosPorSala,
                                Pago pago, List<CompraCandyVistaDTO> conCandy, boolean publica) {
        Funcion f = r.getFuncion();
        List<Asiento> asientos = asientosPorSala.computeIfAbsent(f.getSalaId(), salas::asientosDe);
        return new ReservaVistaDTO(r.getId(), r.getFuncionId(), r.getClienteId(), r.estadoA(reloj.ahora()).name(),
                Fechas.texto(r.getCreadaEn()), Fechas.texto(r.getVenceEn()), r.getCodigo(),
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal().aPesos(),
                r.esCobrable(reloj.ahora()), r.esCancelable(reloj.ahora()),
                vistasCartelera.funcion(f, f.getSala(), asientos),
                vistasCartelera.pelicula(f.getPelicula()),
                vistasSalas.sala(f.getSala(), asientos),
                publica ? vistasUsuarios.clientePublico(r.getCliente()) : vistasUsuarios.cliente(r.getCliente()),
                pago == null ? null : vistasPagos.pago(pago), conCandy);
    }

    private EntradaVistaDTO entrada(Entrada e) {
        return new EntradaVistaDTO(e.asientoId(), e.codigoAsiento(), e.tarifa().name(),
                e.tarifa().requiereAcreditacion(), e.precio().aPesos());
    }

    public BloqueoVistaDTO bloqueo(ButacasBloqueadas bloqueo) {
        return new BloqueoVistaDTO(bloqueo.sesion(), bloqueo.conseguidas(), bloqueo.rechazadas(),
                BloqueoDeButacas.MIENTRAS_ELIGE.toSeconds());
    }
}
