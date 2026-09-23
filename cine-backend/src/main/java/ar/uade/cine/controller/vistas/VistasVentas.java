package ar.uade.cine.controller.vistas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

/**
 * Arma reservas y pagos como los espera el front. Tiene tantos colaboradores porque el
 * ticket junta película, sala, función, cliente y pago, y cada uno vive en otro gestor.
 */
@Component
public class VistasVentas {

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

    /**
     * Un listado con un número fijo de consultas, no cinco por fila (N+1). Los catálogos son
     * chicos y se traen enteros; los pagos pueden ser miles y se piden con un solo {@code IN}.
     * Si las funciones llegan a miles, habrá que paginar o filtrar la precarga por id.
     */
    public List<ReservaVistaDTO> reservas(List<Reserva> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Integer, Funcion> porFuncion = indexar(funciones.listar(), Funcion::getId);
        Map<Integer, Sala> porSala = indexar(salas.listar(), Sala::getId);
        Map<Integer, Pelicula> porPelicula = indexar(cartelera.listar(), Pelicula::getId);
        Map<Integer, Cliente> porCliente = indexar(clientes.listar(), Cliente::getId);
        Map<Integer, Pago> porReserva = indexar(
                pagos.buscarPorReservas(lista.stream().map(Reserva::getId).toList()),
                Pago::getReservaId);

        return lista.stream()
                .map(r -> armar(r, porFuncion, porSala, porPelicula, porCliente, porReserva))
                .toList();
    }

    private static <T> Map<Integer, T> indexar(List<T> elementos, ToIntFunction<T> clave) {
        Map<Integer, T> porId = new HashMap<>();
        for (T elemento : elementos) {
            porId.put(clave.applyAsInt(elemento), elemento);
        }
        return porId;
    }

    /** El mismo DTO que {@link #reserva(Reserva)}, pero leyendo de lo ya precargado. */
    private ReservaVistaDTO armar(Reserva r, Map<Integer, Funcion> porFuncion,
                                  Map<Integer, Sala> porSala, Map<Integer, Pelicula> porPelicula,
                                  Map<Integer, Cliente> porCliente, Map<Integer, Pago> porReserva) {
        Funcion f = porFuncion.get(r.getFuncionId());
        if (f == null) {
            throw new NoEncontrado("No existe la función " + r.getFuncionId());
        }
        Sala sala = porSala.get(f.getSalaId());
        if (sala == null) {
            throw new NoEncontrado("No existe la sala " + f.getSalaId());
        }
        return dto(r, f, sala, porPelicula.get(f.getPeliculaId()), porCliente.get(r.getClienteId()),
                porReserva.get(r.getId()));
    }

    public ReservaVistaDTO reserva(Reserva r) {
        Funcion f = funciones.buscar(r.getFuncionId())
                .orElseThrow(() -> new NoEncontrado("No existe la función " + r.getFuncionId()));
        Sala sala = salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
        return dto(r, f, sala,
                cartelera.buscar(f.getPeliculaId()).orElse(null),
                clientes.buscar(r.getClienteId()).orElse(null),
                pagos.buscarPorReserva(r.getId()).orElse(null));
    }

    /** Película, cliente y pago admiten null: la reserva se muestra igual sin ellos. */
    private ReservaVistaDTO dto(Reserva r, Funcion f, Sala sala, Pelicula pelicula, Cliente cliente, Pago pago) {
        return new ReservaVistaDTO(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                Fechas.texto(r.getCreadaEn()), r.getCodigo(),
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal().aPesos(),
                vistasCartelera.funcion(f),
                pelicula == null ? null : vistasCartelera.pelicula(pelicula),
                vistasSalas.sala(sala),
                cliente == null ? null : vistasUsuarios.cliente(cliente),
                pago == null ? null : pago(pago));
    }

    private EntradaVistaDTO entrada(Entrada e) {
        return new EntradaVistaDTO(e.asientoId(), e.codigoAsiento(), e.tarifa().name(),
                e.precio().aPesos());
    }

    public PagoVistaDTO pago(Pago p) {
        return dto(p, null, null, null);
    }

    /** El arqueo muestra qué se cobró, no solo cuánto: película, cliente y entradas. */
    public PagoVistaDTO pagoDeArqueo(Pago p) {
        Reserva reserva = reservas.buscar(p.getReservaId()).orElse(null);
        if (reserva == null) {
            return pago(p);
        }
        PeliculaVistaDTO pelicula = funciones.buscar(reserva.getFuncionId())
                .flatMap(f -> cartelera.buscar(f.getPeliculaId()))
                .map(vistasCartelera::pelicula)
                .orElse(null);
        return dto(p, pelicula,
                clientes.buscar(reserva.getClienteId()).map(vistasUsuarios::cliente).orElse(null),
                reserva.getCantidadEntradas());
    }

    private static PagoVistaDTO dto(Pago p, PeliculaVistaDTO pelicula, ClienteVistaDTO cliente, Integer entradas) {
        return new PagoVistaDTO(p.getId(), p.getReservaId(), p.getSubtotal().aPesos(), p.getPromocionId(),
                p.getDescuento().aPesos(), p.getMonto().aPesos(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), pelicula, cliente, entradas);
    }
}
