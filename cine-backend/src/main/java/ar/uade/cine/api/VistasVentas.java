package ar.uade.cine.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.api.VistasCartelera.FuncionVista;
import ar.uade.cine.api.VistasCartelera.PeliculaVista;
import ar.uade.cine.api.VistasSalas.SalaVista;
import ar.uade.cine.api.VistasUsuarios.ClienteVista;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Las reservas y los pagos, en la forma que espera el front.
 *
 * <p>Es la vista con más colaboradores del sistema, y no por desprolijidad: el front
 * muestra la reserva como un ticket completo —qué película, en qué sala, a qué hora, de
 * quién y si ya se pagó— y cada uno de esos datos vive en un lado distinto. Reducir esta
 * lista sería cambiar el contrato de la API, no reacomodar código.
 */
public class VistasVentas {

    /** tarifa viaja para que el acomodador sepa si tiene que pedir un carnet. */
    public record EntradaVista(int asientoId, String codigo, String tarifa, double precio) {
    }

    /**
     * {@code codigo} es el del QR y {@code ingresadaEn} cuándo se usó, en null si todavía
     * no entraron: son los dos datos que necesita el que valida en la puerta, y los que
     * va a leer la app del escáner.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReservaVista(int id, int funcionId, int clienteId, String estado, String creadaEn,
                               String codigo, String ingresadaEn,
                               List<EntradaVista> entradas, int cantidadEntradas, double total,
                               FuncionVista funcion, PeliculaVista pelicula, SalaVista sala,
                               ClienteVista cliente, PagoVista pago) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PagoVista(int id, int reservaId, double subtotal, Integer promocionId,
                            double descuento, double monto, String medio, String fecha,
                            String codigoAutorizacion, PeliculaVista pelicula, ClienteVista cliente,
                            Integer entradas) {
    }

    private final GestorFunciones funciones;
    private final GestorSalas salas;
    private final GestorCartelera cartelera;
    private final GestorClientes clientes;
    private final GestorPagos pagos;
    private final GestorReservas reservas;
    private final VistasCartelera vistasCartelera;
    private final VistasSalas vistasSalas;
    private final VistasUsuarios vistasUsuarios;

    public VistasVentas(GestorFunciones funciones, GestorSalas salas, GestorCartelera cartelera,
                        GestorClientes clientes, GestorPagos pagos, GestorReservas reservas,
                        VistasCartelera vistasCartelera, VistasSalas vistasSalas,
                        VistasUsuarios vistasUsuarios) {
        this.funciones = funciones;
        this.salas = salas;
        this.cartelera = cartelera;
        this.clientes = clientes;
        this.pagos = pagos;
        this.reservas = reservas;
        this.vistasCartelera = vistasCartelera;
        this.vistasSalas = vistasSalas;
        this.vistasUsuarios = vistasUsuarios;
    }

    /** La reserva con todo lo que necesita el ticket: función, película, sala y cliente. */
    public ReservaVista reserva(Reserva r) {
        Funcion f = funciones.buscar(r.getFuncionId())
                .orElseThrow(() -> new NoEncontrado("No existe la función " + r.getFuncionId()));
        Sala sala = salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
        return new ReservaVista(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                Fechas.texto(r.getCreadaEn()), r.getCodigo(),
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal(),
                vistasCartelera.funcion(f),
                cartelera.buscar(f.getPeliculaId()).map(vistasCartelera::pelicula).orElse(null),
                vistasSalas.sala(sala),
                clientes.buscar(r.getClienteId()).map(vistasUsuarios::cliente).orElse(null),
                pagos.buscarPorReserva(r.getId()).map(this::pago).orElse(null));
    }

    private EntradaVista entrada(Entrada e) {
        return new EntradaVista(e.asientoId(), e.codigoAsiento(), e.tarifa().name(), e.precio());
    }

    public PagoVista pago(Pago p) {
        return new PagoVista(p.getId(), p.getReservaId(), p.getSubtotal(), p.getPromocionId(),
                p.getDescuento(), p.getMonto(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), null, null, null);
    }

    /**
     * El arqueo muestra qué se cobró, no solo cuánto: cada pago viaja con la película y
     * el cliente de su reserva, y con cuántas entradas se llevó.
     */
    public PagoVista pagoDeArqueo(Pago p) {
        Reserva reserva = reservas.buscar(p.getReservaId()).orElse(null);
        if (reserva == null) {
            return pago(p);
        }
        PeliculaVista pelicula = funciones.buscar(reserva.getFuncionId())
                .flatMap(f -> cartelera.buscar(f.getPeliculaId()))
                .map(vistasCartelera::pelicula)
                .orElse(null);
        return new PagoVista(p.getId(), p.getReservaId(), p.getSubtotal(), p.getPromocionId(),
                p.getDescuento(), p.getMonto(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), pelicula,
                clientes.buscar(reserva.getClienteId()).map(vistasUsuarios::cliente).orElse(null),
                reserva.getCantidadEntradas());
    }
}
