package ar.uade.cine.controller.vistas;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.model.dinero.Dinero;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.usuarios.Cliente;

/**
 * Arma las reservas y los pagos en la forma que espera el front.
 *
 * <p>Es el ensamblador con más colaboradores del sistema, y no por desprolijidad: el front
 * muestra la reserva como un ticket completo —qué película, en qué sala, a qué hora, de
 * quién y si ya se pagó— y cada uno de esos datos vive en un lado distinto. Reducir esta
 * lista sería cambiar el contrato de la API, no reacomodar código.
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
     * Un listado entero de reservas, armado con un número fijo de consultas en vez de una
     * por fila.
     *
     * <p><strong>Por qué existe:</strong> {@link #reserva(Reserva)} necesita cinco cosas
     * que no están en la reserva —su función, la sala, la película, el cliente y el pago—
     * y las pide una por una. Para una reserva suelta está bien; para el listado del panel
     * significaba cinco consultas <em>por fila</em>: doscientas reservas eran mil consultas
     * para dibujar una tabla. Es el problema que se conoce como N+1, y no se nota en
     * desarrollo con cuatro reservas de prueba.
     *
     * <p>Acá los catálogos se traen enteros una sola vez y se indexan por id. Son listas
     * chicas y acotadas por el negocio —un cine tiene unas pocas salas y unas decenas de
     * películas— así que traerlas completas cuesta menos que ir a buscarlas de a una. Los
     * pagos son la excepción: pueden ser miles, así que se piden solo los de estas
     * reservas, con un único {@code IN}.
     *
     * <p>Tiene un techo, y conviene tenerlo escrito: cuando las funciones pasen de unos
     * miles, traerlas todas para dibujar una página dejará de convenir y habrá que paginar
     * el listado o filtrar la precarga por los ids que realmente aparecen.
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
        Pelicula pelicula = porPelicula.get(f.getPeliculaId());
        Cliente cliente = porCliente.get(r.getClienteId());
        Pago pago = porReserva.get(r.getId());

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

    /** La reserva con todo lo que necesita el ticket: función, película, sala y cliente. */
    public ReservaVistaDTO reserva(Reserva r) {
        Funcion f = funciones.buscar(r.getFuncionId())
                .orElseThrow(() -> new NoEncontrado("No existe la función " + r.getFuncionId()));
        Sala sala = salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
        return new ReservaVistaDTO(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                Fechas.texto(r.getCreadaEn()), r.getCodigo(),
                Fechas.texto(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal().aPesos(),
                vistasCartelera.funcion(f),
                cartelera.buscar(f.getPeliculaId()).map(vistasCartelera::pelicula).orElse(null),
                vistasSalas.sala(sala),
                clientes.buscar(r.getClienteId()).map(vistasUsuarios::cliente).orElse(null),
                pagos.buscarPorReserva(r.getId()).map(this::pago).orElse(null));
    }

    private EntradaVistaDTO entrada(Entrada e) {
        return new EntradaVistaDTO(e.asientoId(), e.codigoAsiento(), e.tarifa().name(),
                e.precio().aPesos());
    }

    public PagoVistaDTO pago(Pago p) {
        return new PagoVistaDTO(p.getId(), p.getReservaId(), p.getSubtotal().aPesos(), p.getPromocionId(),
                p.getDescuento().aPesos(), p.getMonto().aPesos(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), null, null, null);
    }

    /**
     * El arqueo muestra qué se cobró, no solo cuánto: cada pago viaja con la película y
     * el cliente de su reserva, y con cuántas entradas se llevó.
     */
    public PagoVistaDTO pagoDeArqueo(Pago p) {
        Reserva reserva = reservas.buscar(p.getReservaId()).orElse(null);
        if (reserva == null) {
            return pago(p);
        }
        PeliculaVistaDTO pelicula = funciones.buscar(reserva.getFuncionId())
                .flatMap(f -> cartelera.buscar(f.getPeliculaId()))
                .map(vistasCartelera::pelicula)
                .orElse(null);
        return new PagoVistaDTO(p.getId(), p.getReservaId(), p.getSubtotal().aPesos(), p.getPromocionId(),
                p.getDescuento().aPesos(), p.getMonto().aPesos(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), pelicula,
                clientes.buscar(reserva.getClienteId()).map(vistasUsuarios::cliente).orElse(null),
                reserva.getCantidadEntradas());
    }
}
