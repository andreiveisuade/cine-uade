package ar.uade.cine.service.informes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.comprobantes.GeneradorBordero;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
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

/**
 * Los informes que se cortan <strong>por función</strong>: el borderó para el INCAA y
 * cuánto dejó la función sumando entradas y candy.
 *
 * <p>Aparte de {@code GestorPagos} porque el sujeto es otro: cobrar opera sobre una
 * reserva; informar es una lectura que cruza película, sala, reservas, cobros y candy sin
 * escribir nada. Por eso tiene tantos repositorios, y todos de lectura.
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
    private final Reloj reloj;

    public GestorInformes(FuncionRepository funcionRepository, PeliculaRepository peliculaRepository, SalaRepository salaRepository,
                          ReservaRepository reservaRepository, PagoRepository pagoRepository, CompraCandyRepository compraCandyRepository,
                          GeneradorBordero generadorBordero, Reloj reloj) {
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.compraCandyRepository = compraCandyRepository;
        this.generadorBordero = generadorBordero;
        this.reloj = reloj;
    }

    /**
     * Qué se vendió para esa función y a qué valor. Se declara lo que se <strong>cobró</strong>:
     * la fuente es el pago, no el estado de la reserva, porque una reserva sin pagar
     * retiene butacas pero no vendió nada.
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
        Map<Integer, Pago> pagosPorReserva = new HashMap<>();
        for (Pago cobro : pagoRepository.findByReservaIdIn(reservas.stream().map(Reserva::getId).toList())) {
            pagosPorReserva.put(cobro.getReservaId(), cobro);
        }

        for (Reserva reserva : reservas) {
            Pago pago = pagosPorReserva.get(reserva.getId());
            if (pago == null) {
                continue;
            }
            for (Entrada entrada : reserva.getEntradas()) {
                Bordero.TotalPorTarifa acumulado = porTarifa.getOrDefault(entrada.tarifa(), SIN_ENTRADAS);
                porTarifa.put(entrada.tarifa(), new Bordero.TotalPorTarifa(acumulado.cantidad() + 1,
                        acumulado.total().mas(entrada.precio())));
                espectadores++;
            }
            // El desglose por tarifa va a precio de lista y los totales con el pago, que es
            // el único que sabe cuánto sacó la promoción (es sobre el total, no por butaca).
            bruta = bruta.mas(pago.getSubtotal());
            descuentos = descuentos.mas(pago.getDescuento());
            neta = neta.mas(pago.getMonto());
        }

        return new Bordero(funcionId, pelicula.getTitulo(), sala.getNombre(), funcion.getInicio(),
                reloj.ahora(), espectadores,
                bruta, descuentos, neta, porTarifa);
    }

    /** Emite el borderó a un archivo y devuelve lo que se escribió, para mostrarlo sin volver a pedirlo. */
    public Bordero exportarBordero(int funcionId) {
        Bordero bordero = borderoDe(funcionId);
        generadorBordero.emitir(bordero);
        // Una declaración jurada deja rastro: es lo que se busca cuando el organismo reclama.
        LOG.info("bordero funcion {} · {} espectadores · bruto {} · neto {}",
                funcionId, bordero.espectadores(), bordero.recaudacionBruta(),
                bordero.recaudacionNeta());
        return bordero;
    }

    /**
     * Cuánto dejó la función entre las dos cajas. El candy de mostrador queda afuera a
     * propósito: solo se atribuye a una función la compra que tiene {@code reservaId}.
     * Repartir el mostrador entre las funciones del día sería inventar el dato; esa plata
     * se cuenta en el arqueo del día ({@link GestorCaja#totalCandyDe}). Por eso la suma de
     * los informes de un día es menor o igual al arqueo, y la diferencia es el mostrador.
     */
    public InformeFuncion informeDe(int funcionId) {
        Bordero bordero = borderoDe(funcionId);

        // Sin mirar el estado de la reserva: una compra del candy nace cobrada.
        List<Integer> reservas = reservaRepository.findByFuncionId(funcionId).stream()
                .map(Reserva::getId).toList();
        List<CompraCandy> compras = compraCandyRepository.findByReservaIdIn(reservas);
        Dinero candy = Dinero.sumar(compras.stream().map(CompraCandy::getTotal).toList());

        return new InformeFuncion(bordero, compras.size(), candy, bordero.recaudacionNeta().mas(candy));
    }

    /** El informe de una función que no existe no es una lista vacía: es un pedido mal hecho. */
    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
    }
}
