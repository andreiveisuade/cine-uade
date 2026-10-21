package ar.uade.cine.controller.ventas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.cartelera.VistasCartelera;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.salas.VistasSalas;
import ar.uade.cine.controller.usuarios.VistasUsuarios;
import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.BloqueoDeButacas;
import ar.uade.cine.service.ventas.ButacasBloqueadas;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorPagos;

// Arma los JSON de reservas y del bloqueo de butacas; Assembler que compone Cartelera, Salas y Pagos.
// Un listado cuesta consultas fijas: reservas con detalle en una, pagos en otra, butacas por sala;
// VistasVentasTest cuenta las sentencias
@Component
@RequiredArgsConstructor
public class VistasReservas {

    private final GestorPagos pagos;
    private final GestorSalas salas;
    private final ConsultasReservas reservas;
    private final VistasCartelera vistasCartelera;
    private final VistasSalas vistasSalas;
    private final VistasUsuarios vistasUsuarios;
    private final VistasPagos vistasPagos;
    private final Reloj reloj;

    public List<ReservaVistaDTO> reservas(List<Reserva> lista) {
        return armar(lista);
    }

    public ReservaVistaDTO reserva(Reserva r) {
        return armar(List.of(r)).get(0);
    }

    private List<ReservaVistaDTO> armar(List<Reserva> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = lista.stream().map(Reserva::getId).toList();
        Map<Integer, Reserva> conDetalle = reservas.conDetalle(ids).stream()
                .collect(Collectors.toMap(Reserva::getId, Function.identity()));
        Map<Integer, Pago> porReserva = pagos.buscarPorReservas(ids).stream()
                .collect(Collectors.toMap(Pago::getReservaId, Function.identity()));
        Map<Integer, List<Asiento>> asientosPorSala = new HashMap<>();

        return lista.stream()
                .map(r -> {
                    return dto(conDetalle.get(r.getId()), asientosPorSala, porReserva.get(r.getId()));
                })
                .toList();
    }

    private ReservaVistaDTO dto(Reserva r, Map<Integer, List<Asiento>> asientosPorSala,
                                Pago pago) {
        Funcion f = r.getFuncion();
        List<Asiento> asientos = asientosPorSala.computeIfAbsent(f.getSalaId(), salas::asientosDe);
        return new ReservaVistaDTO(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                Fechas.texto(r.getCreadaEn()), r.getCodigo(),
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal().aPesos(),
                r.esCobrable(reloj.ahora()), r.esCancelable(reloj.ahora()),
                vistasCartelera.funcion(f, f.getSala(), asientos),
                vistasCartelera.pelicula(f.getPelicula()),
                vistasSalas.sala(f.getSala(), asientos),
                vistasUsuarios.cliente(r.getCliente()),
                pago == null ? null : vistasPagos.pago(pago));
    }

    private EntradaVistaDTO entrada(Entrada e) {
        return new EntradaVistaDTO(e.asientoId(), e.codigoAsiento(), e.tarifa().name(),
                e.precio().aPesos());
    }

    public BloqueoVistaDTO bloqueo(ButacasBloqueadas bloqueo) {
        return new BloqueoVistaDTO(bloqueo.sesion(), bloqueo.conseguidas(), bloqueo.rechazadas(),
                BloqueoDeButacas.MIENTRAS_ELIGE.toSeconds());
    }
}
