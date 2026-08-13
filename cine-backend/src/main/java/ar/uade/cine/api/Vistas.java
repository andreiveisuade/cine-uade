package ar.uade.cine.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Traduce el dominio a la forma exacta que espera el frontend. Es lo único de la capa
 * HTTP que conoce el shape del JSON: los handlers arman la llamada al gestor y delegan
 * acá el armado de la respuesta.
 *
 * <p>No decide nada: para saber si una butaca está ocupada le pregunta a GestorReservas,
 * y el precio de cada una sale de CalculadoraPrecio. Las reglas siguen viviendo en la
 * capa de servicio.
 */
public class Vistas {

    /** El contrato pide ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final GestorCartelera cartelera;
    private final GestorSalas salas;
    private final GestorFunciones funciones;
    private final GestorReservas reservas;
    private final GestorPagos pagos;
    private final GestorClientes clientes;
    private final CalculadoraPrecio calculadora = new CalculadoraPrecio();

    public Vistas(GestorCartelera cartelera, GestorSalas salas, GestorFunciones funciones,
                  GestorReservas reservas, GestorPagos pagos, GestorClientes clientes) {
        this.cartelera = cartelera;
        this.salas = salas;
        this.funciones = funciones;
        this.reservas = reservas;
        this.pagos = pagos;
        this.clientes = clientes;
    }

    // ---------- formas del JSON ----------
    // Los campos que sobran en un endpoint viajan en null y NON_NULL los saca de la
    // respuesta: así una función embebida en el listado no arrastra el mapa de butacas.

    public record PeliculaVista(int id, String titulo, int duracionMinutos, List<String> generos,
                                String clasificacion, String posterUrl, String director, int anio,
                                String idiomaOriginal, String sinopsis, boolean enCartelera) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SalaVista(int id, String nombre, String tipo, List<Integer> butacasPorFila,
                            int filas, int capacidadSala, List<AsientoVista> asientos) {
    }

    /**
     * {@code estado} es de la butaca y vale para todas las funciones; {@code ocupado}
     * es de esta función. Van separados porque el front los pinta distinto. Sin función
     * de por medio, ocupado y precio no aplican y no se mandan.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AsientoVista(int id, int salaId, int fila, int numero, String codigo,
                               String tipo, String estado, Boolean ocupado, Double precio) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FuncionVista(int id, int peliculaId, int salaId, String inicio, String idioma,
                               String proyeccion, double precio, double precioDesde, SalaVista sala,
                               PeliculaVista pelicula, List<AsientoVista> asientos, Integer libres) {
    }

    /** tarifa viaja para que el acomodador sepa si tiene que pedir un carnet. */
    public record EntradaVista(int asientoId, String codigo, String tarifa, double precio) {
    }

    public record ClienteVista(int id, String nombre, String email) {
    }

    /** El administrador que devuelve el login: sin el hash de la contraseña. */
    public record EmpleadoVista(int id, String nombre, String email, String rol) {
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
    public record PagoVista(int id, int reservaId, double monto, String medio, String fecha,
                            String codigoAutorizacion, PeliculaVista pelicula, ClienteVista cliente,
                            Integer entradas) {
    }

    // ---------- armado ----------

    public PeliculaVista pelicula(Pelicula p) {
        return new PeliculaVista(p.getId(), p.getTitulo(), p.getDuracionMinutos(),
                p.getGeneros().stream().map(Enum::name).toList(),
                p.getClasificacion().name(), p.getPosterUrl(), p.getDirector(), p.getAnio(),
                p.getIdiomaOriginal(), p.getSinopsis(), p.estaEnCartelera());
    }

    /** La sala sola, para embeberla en una función. */
    public SalaVista sala(Sala s) {
        return sala(s, salas.asientosDe(s.getId()), false);
    }

    /** La sala con el detalle de cada butaca, que es lo que necesita el ABM de salas. */
    public SalaVista salaConButacas(Sala s) {
        return sala(s, salas.asientosDe(s.getId()), true);
    }

    private SalaVista sala(Sala s, List<Asiento> asientos, boolean conButacas) {
        List<Integer> distribucion = butacasPorFila(asientos);
        List<AsientoVista> detalle = conButacas
                ? asientos.stream().map(this::asiento).toList()
                : null;
        return new SalaVista(s.getId(), s.getNombre(), s.getTipo().name(), distribucion,
                distribucion.size(), asientos.size(), detalle);
    }

    /**
     * La distribución no se guarda en ningún lado: se reconstruye contando las butacas
     * de cada fila, que son la única fuente de verdad de cómo es la sala.
     */
    private List<Integer> butacasPorFila(List<Asiento> asientos) {
        return asientos.stream()
                .collect(Collectors.groupingBy(Asiento::getFila, TreeMap::new, Collectors.counting()))
                .values().stream()
                .map(Long::intValue)
                .toList();
    }

    private AsientoVista asiento(Asiento a) {
        return new AsientoVista(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(), null, null);
    }

    // El mapa de butacas muestra el precio de tarifa general: la tarifa de cada persona
    // se elige recién al reservar, y de ahí para abajo el precio solo puede bajar.
    private AsientoVista asiento(Asiento a, Funcion funcion, Sala sala, Set<Integer> ocupados) {
        return new AsientoVista(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(),
                ocupados.contains(a.getId()),
                calculadora.precioDe(funcion, sala, a, TipoTarifa.GENERAL));
    }

    /** Para el listado del cliente: la función con su sala, sin el mapa de butacas. */
    public FuncionVista funcion(Funcion f) {
        return funcion(f, false, false);
    }

    /** Para el listado del encargado, que muestra qué película va en cada función. */
    public FuncionVista funcionConPelicula(Funcion f) {
        return funcion(f, true, false);
    }

    /** El mapa de butacas: cada asiento de la sala con su precio y si está tomado acá. */
    public FuncionVista funcionConButacas(Funcion f) {
        return funcion(f, true, true);
    }

    private FuncionVista funcion(Funcion f, boolean conPelicula, boolean conButacas) {
        Sala sala = salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
        List<Asiento> asientos = salas.asientosDe(sala.getId());

        List<AsientoVista> butacas = null;
        Integer libres = null;
        if (conButacas) {
            Set<Integer> ocupados = reservas.asientosOcupados(f.getId());
            butacas = asientos.stream().map(a -> asiento(a, f, sala, ocupados)).toList();
            libres = reservas.lugaresLibres(f.getId());
        }
        PeliculaVista pelicula = conPelicula
                ? cartelera.buscar(f.getPeliculaId()).map(this::pelicula).orElse(null)
                : null;

        return new FuncionVista(f.getId(), f.getPeliculaId(), f.getSalaId(), fecha(f.getInicio()),
                f.getVersion().name(), f.getProyeccion().name(), f.getPrecio(),
                calculadora.precioBaseEnSala(f, sala), sala(sala, asientos, false),
                pelicula, butacas, libres);
    }

    /** La reserva con todo lo que necesita el ticket: función, película, sala y cliente. */
    public ReservaVista reserva(Reserva r) {
        Funcion f = funciones.buscar(r.getFuncionId())
                .orElseThrow(() -> new NoEncontrado("No existe la función " + r.getFuncionId()));
        Sala sala = salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
        return new ReservaVista(r.getId(), r.getFuncionId(), r.getClienteId(), r.getEstado().name(),
                fecha(r.getCreadaEn()), r.getCodigo(),
                r.getIngresadaEn() == null ? null : fecha(r.getIngresadaEn()),
                r.getEntradas().stream().map(this::entrada).toList(),
                r.getCantidadEntradas(), r.getTotal(),
                funcion(f),
                cartelera.buscar(f.getPeliculaId()).map(this::pelicula).orElse(null),
                sala(sala),
                clientes.buscar(r.getClienteId()).map(this::cliente).orElse(null),
                pagos.buscarPorReserva(r.getId()).map(this::pago).orElse(null));
    }

    private EntradaVista entrada(Entrada e) {
        return new EntradaVista(e.asientoId(), e.codigoAsiento(), e.tarifa().name(), e.precio());
    }

    public ClienteVista cliente(Cliente c) {
        return new ClienteVista(c.getId(), c.getNombre(), c.getEmail());
    }

    public EmpleadoVista administrador(Empleado a) {
        return new EmpleadoVista(a.getId(), a.getNombre(), a.getEmail(), a.getRol().name());
    }

    public PagoVista pago(Pago p) {
        return new PagoVista(p.getId(), p.getReservaId(), p.getMonto(), p.getMedio().name(),
                fecha(p.getFecha()), p.getCodigoAutorizacion(), null, null, null);
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
                .map(this::pelicula)
                .orElse(null);
        return new PagoVista(p.getId(), p.getReservaId(), p.getMonto(), p.getMedio().name(),
                fecha(p.getFecha()), p.getCodigoAutorizacion(), pelicula,
                clientes.buscar(reserva.getClienteId()).map(this::cliente).orElse(null),
                reserva.getCantidadEntradas());
    }

    private String fecha(LocalDateTime momento) {
        return momento == null ? null : momento.format(ISO);
    }
}
