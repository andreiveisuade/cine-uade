package ar.uade.cine.controller.ventas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.CheckoutVistaDTO;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.controller.cartelera.VistasCartelera;
import ar.uade.cine.controller.salas.VistasSalas;
import ar.uade.cine.controller.usuarios.VistasUsuarios;

// Un listado de N reservas cuesta un número fijo de consultas: las reservas con su función,
// película, sala y cliente en una (ConsultasReservas#conDetalle), sus pagos en otra, y las
// butacas una vez por sala, no por fila. VistasVentasTest cuenta las sentencias.
@Component
@RequiredArgsConstructor
public class VistasVentas {

    private final GestorPagos pagos;
    private final GestorSalas salas;
    private final ConsultasReservas reservas;
    private final VistasCartelera vistasCartelera;
    private final VistasSalas vistasSalas;
    private final VistasUsuarios vistasUsuarios;
    private final Reloj reloj;

    public List<ReservaVistaDTO> reservas(List<Reserva> lista) {
        return armar(lista, true);
    }

    // Sin el código de acceso: es la credencial del cliente y esta vista sale por una ruta pública.
    public List<ReservaVistaDTO> reservasSinCodigo(List<Reserva> lista) {
        return armar(lista, false);
    }

    public ReservaVistaDTO reserva(Reserva r) {
        return armar(List.of(r), true).get(0);
    }

    private List<ReservaVistaDTO> armar(List<Reserva> lista, boolean conCodigo) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = lista.stream().map(Reserva::getId).toList();
        Map<Integer, Reserva> conDetalle = indexar(reservas.conDetalle(ids), Reserva::getId);
        Map<Integer, Pago> porReserva = indexar(pagos.buscarPorReservas(ids), Pago::getReservaId);
        Map<Integer, List<Asiento>> asientosPorSala = new HashMap<>();

        return lista.stream()
                .map(r -> {
                    Reserva completa = conDetalle.get(r.getId());
                    if (completa == null) {
                        throw new RecursoNoEncontrado("No existe la reserva " + r.getId());
                    }
                    return dto(completa, conCodigo, asientosPorSala, porReserva.get(r.getId()));
                })
                .toList();
    }

    private static <T> Map<Integer, T> indexar(List<T> elementos, ToIntFunction<T> clave) {
        Map<Integer, T> porId = new HashMap<>();
        for (T elemento : elementos) {
            porId.put(clave.applyAsInt(elemento), elemento);
        }
        return porId;
    }

    private ReservaVistaDTO dto(Reserva r, boolean conCodigo, Map<Integer, List<Asiento>> asientosPorSala,
                                Pago pago) {
        Funcion f = r.getFuncion();
        List<Asiento> asientos = asientosPorSala.computeIfAbsent(f.getSalaId(), salas::asientosDe);
        return new ReservaVistaDTO(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                Fechas.texto(r.getCreadaEn()), conCodigo ? r.getCodigo() : null,
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal().aPesos(),
                r.esCobrable(reloj.ahora()), r.esCancelable(),
                vistasCartelera.funcion(f, f.getSala(), asientos),
                vistasCartelera.pelicula(f.getPelicula()),
                vistasSalas.sala(f.getSala(), asientos),
                vistasUsuarios.cliente(r.getCliente()),
                pago == null ? null : pago(pago));
    }

    private EntradaVistaDTO entrada(Entrada e) {
        return new EntradaVistaDTO(e.asientoId(), e.codigoAsiento(), e.tarifa().name(),
                e.precio().aPesos());
    }

    public PagoVistaDTO pago(Pago p) {
        return dto(p, null, null, null);
    }

    // El arqueo sí dice qué se vendió y a quién. Por el mismo camino que las reservas: una
    // consulta para todas, no tres por pago.
    public List<PagoVistaDTO> pagosDeArqueo(List<Pago> lista) {
        Map<Integer, Reserva> porId = indexar(
                reservas.conDetalle(lista.stream().map(Pago::getReservaId).toList()), Reserva::getId);
        return lista.stream()
                .map(p -> {
                    Reserva reserva = porId.get(p.getReservaId());
                    if (reserva == null) {
                        return pago(p);
                    }
                    return dto(p, vistasCartelera.pelicula(reserva.getFuncion().getPelicula()),
                            vistasUsuarios.cliente(reserva.getCliente()), reserva.getCantidadEntradas());
                })
                .toList();
    }

    private static PagoVistaDTO dto(Pago p, PeliculaVistaDTO pelicula, ClienteVistaDTO cliente, Integer entradas) {
        return new PagoVistaDTO(p.getId(), p.getReservaId(), p.getSubtotal().aPesos(), p.getPromocionId(),
                p.getDescuento().aPesos(), p.getMonto().aPesos(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), pelicula, cliente, entradas);
    }

    public CheckoutVistaDTO checkout(PasarelaPagos.Checkout c) {
        return new CheckoutVistaDTO(c.id(), c.reservaId(), c.medio().name(), c.monto().aPesos(),
                c.urlPago(), c.codigoQr());
    }

    public BloqueoVistaDTO bloqueo(String sesion, Ocupacion.Bloqueo bloqueo) {
        return new BloqueoVistaDTO(sesion, bloqueo.conseguidas(), bloqueo.rechazadas(),
                Ocupacion.MIENTRAS_ELIGE.toSeconds());
    }
}
