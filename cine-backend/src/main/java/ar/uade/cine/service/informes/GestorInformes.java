package ar.uade.cine.service.informes;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.comprobantes.GeneradorBordero;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PagoRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Los informes que se cortan <strong>por función</strong>: el borderó que se le declara al
 * INCAA y cuánto dejó esa función sumando entradas y candy.
 *
 * <p>Es un gestor aparte y no dos métodos más de {@link GestorPagos} por una razón de
 * sujeto: cobrar es una operación sobre <em>una</em> reserva y necesita saber de estados,
 * vencimientos y promociones; informar es una lectura que cruza película, sala, reservas,
 * cobros y candy sin escribir nada del negocio. Metidos en el mismo gestor, el que cobra
 * habría terminado cargando con la película y la sala para poder titular un informe.
 *
 * <p>Por eso también es el gestor con más DAOs después de GestorReservas, y todos de
 * lectura: el encabezado sale de película y sala, las entradas de reservas y pagos, y la
 * barra de las compras del candy.
 */
@Service
public class GestorInformes {

    private static final Logger LOG = LoggerFactory.getLogger(GestorInformes.class);

    private static final Bordero.TotalPorTarifa SIN_ENTRADAS =
            new Bordero.TotalPorTarifa(0, Dinero.CERO);

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final CompraCandyRepository compraCandyRepository;
    private final GeneradorBordero generadorBordero;

    public GestorInformes(FuncionRepository funcionRepository, PeliculaRepository peliculaRepository, SalaRepository salaRepository,
                          ReservaRepository reservaRepository, PagoRepository pagoRepository, CompraCandyRepository compraCandyRepository,
                          GeneradorBordero generadorBordero) {
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.compraCandyRepository = compraCandyRepository;
        this.generadorBordero = generadorBordero;
    }

    /**
     * Qué se vendió para esa función y a qué valor.
     *
     * <p>Lo que se declara es lo que se <strong>cobró</strong>, y por eso la fuente es el
     * pago y no el estado de la reserva. Una reserva sin pagar retiene butacas pero no
     * vendió ninguna entrada, y contarla infla la declaración con plata que nunca entró
     * —y que, si nadie la paga, vuelve a estar a la venta cuando expire (R17)—.
     */
    public Bordero borderoDe(int funcionId) {
        Funcion funcion = buscarFuncion(funcionId);
        Pelicula pelicula = peliculaRepository.findById(funcion.getPeliculaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la película " + funcion.getPeliculaId()));
        Sala sala = salaRepository.findById(funcion.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la sala " + funcion.getSalaId()));

        Map<TipoTarifa, Bordero.TotalPorTarifa> porTarifa = new EnumMap<>(TipoTarifa.class);
        int espectadores = 0;
        Dinero bruta = Dinero.CERO;
        Dinero descuentos = Dinero.CERO;
        Dinero neta = Dinero.CERO;
        List<Reserva> reservas = reservaRepository.findByFuncionId(funcionId);
        // Los pagos de todas las reservas de una sola consulta. Pedirlos de a uno adentro
        // del bucle era una consulta por reserva de la función.
        Map<Integer, Pago> pagosPorReserva = new HashMap<>();
        for (Pago cobro : pagoRepository.findByReservaIdIn(reservas.stream().map(Reserva::getId).toList())) {
            pagosPorReserva.put(cobro.getReservaId(), cobro);
        }

        for (Reserva reserva : reservas) {
            Optional<Pago> pago = Optional.ofNullable(pagosPorReserva.get(reserva.getId()));
            if (pago.isEmpty()) {
                continue;
            }
            for (Entrada entrada : reserva.getEntradas()) {
                Bordero.TotalPorTarifa acumulado = porTarifa.getOrDefault(entrada.tarifa(), SIN_ENTRADAS);
                porTarifa.put(entrada.tarifa(), new Bordero.TotalPorTarifa(acumulado.cantidad() + 1,
                        acumulado.total().mas(entrada.precio())));
                espectadores++;
            }
            // El desglose por tarifa se arma con el precio de lista de cada butaca y los
            // totales con el pago: son la misma plata mirada por dos lados, y el pago es
            // el único que sabe cuánto sacó la promoción, que es sobre el total y no sobre
            // una butaca.
            bruta = bruta.mas(pago.get().getSubtotal());
            descuentos = descuentos.mas(pago.get().getDescuento());
            neta = neta.mas(pago.get().getMonto());
        }

        return new Bordero(funcionId, pelicula.getTitulo(), sala.getNombre(), funcion.getInicio(),
                LocalDateTime.now(), espectadores,
                bruta, descuentos, neta, porTarifa);
    }

    /**
     * Emite el borderó a un archivo, que es lo que se sube al INCAA. Devuelve el mismo
     * informe que se escribió: quien lo pidió por pantalla no tiene que volver a pedirlo
     * para mostrar lo que declaró.
     */
    public Bordero exportarBordero(int funcionId) {
        Bordero bordero = borderoDe(funcionId);
        generadorBordero.emitir(bordero);
        // La emisión de una declaración jurada se anota: es el rastro de qué se declaró y
        // cuándo, que es justo lo que se busca cuando el organismo reclama una diferencia.
        LOG.info("bordero funcion {} · {} espectadores · bruto {} · neto {}",
                funcionId, bordero.espectadores(), bordero.recaudacionBruta(),
                bordero.recaudacionNeta());
        return bordero;
    }

    /**
     * Cuánto dejó la función entre las dos cajas del cine.
     *
     * <p><strong>El candy de mostrador queda afuera, a propósito.</strong> Una
     * {@link CompraCandy} solo se puede atribuir a una función si tiene {@code reservaId}
     * —el «¿desea agregar pochoclos?» de después de comprar la entrada—, porque esa reserva
     * dice de qué función se trata. La venta suelta del mostrador no lo dice: quien compra
     * un balde puede estar yendo a cualquiera de las cuatro funciones de las 22:00, o a
     * ninguna. Repartirla entre las funciones del día sería inventar el dato, así que esa
     * plata se cuenta donde sí es cierta: en el arqueo del día
     * ({@link GestorCandy#totalVendido}), que suma la barra completa. La consecuencia hay
     * que tenerla clara al leer los números: la suma de los informes de todas las funciones
     * de un día es menor o igual al arqueo de ese día, y la diferencia es el mostrador.
     */
    public InformeFuncion informeDe(int funcionId) {
        Bordero bordero = borderoDe(funcionId);

        int compras = 0;
        Dinero candy = Dinero.CERO;
        for (Reserva reserva : reservaRepository.findByFuncionId(funcionId)) {
            // Sin mirar el estado de la reserva: una compra del candy nace cobrada, así
            // que esa plata entró aunque después la reserva se cancelara.
            for (CompraCandy compra : compraCandyRepository.findByReservaId(reserva.getId())) {
                compras++;
                candy = candy.mas(compra.getTotal());
            }
        }

        Dinero total = bordero.recaudacionNeta().mas(candy);
        return new InformeFuncion(bordero, compras, candy, total);
    }

    /** El informe de una función que no existe no es una lista vacía: es un pedido mal hecho. */
    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
    }
}
