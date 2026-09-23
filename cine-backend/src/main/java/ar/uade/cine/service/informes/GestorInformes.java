package ar.uade.cine.service.informes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import ar.uade.cine.service.RecursoNoEncontrado;

@Service
@Transactional(readOnly = true)
public class GestorInformes {

    private static final Logger LOG = LoggerFactory.getLogger(GestorInformes.class);

    private static final Bordero.TotalPorTarifa SIN_ENTRADAS =
            new Bordero.TotalPorTarifa(0, Dinero.CERO);

    public static final int MAXIMO_DIAS_DECLARACION = 31;

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

    public Bordero borderoDe(int funcionId) {
        Funcion funcion = buscarFuncion(funcionId);
        Pelicula pelicula = peliculaRepository.findById(funcion.getPeliculaId())
                .orElseThrow(() -> new RecursoNoEncontrado(
                        "No existe la película " + funcion.getPeliculaId()));
        Sala sala = salaRepository.findById(funcion.getSalaId())
                .orElseThrow(() -> new RecursoNoEncontrado(
                        "No existe la sala " + funcion.getSalaId()));

        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
        return bordero(funcion, pelicula.getTitulo(), sala.getNombre(), reservas,
                pagosPorReserva(reservas));
    }

    // Por fecha de la función y no del cobro: el INCAA declara espectadores de lo exhibido en la semana.
    public DeclaracionJurada declaracionJurada(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            LocalDate juevesDeEstaSemana = reloj.hoy().with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
            desde = juevesDeEstaSemana.minusWeeks(1);
            hasta = juevesDeEstaSemana.minusDays(1);
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Hay que indicar desde y hasta, o ninguna de las dos para la última semana cinematográfica");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        // El archivo se arma entero en memoria: el tope lo acota, y un mes cubre cualquier cierre del INCAA.
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS_DECLARACION) {
            throw new IllegalArgumentException(
                    "El período no puede superar los " + MAXIMO_DIAS_DECLARACION + " días");
        }

        List<Reserva> cobradas = reservaRepository.findCobradasDeFuncionesEntre(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        Map<Integer, Pago> pagos = pagosPorReserva(cobradas);
        Map<Integer, List<Reserva>> porFuncion = new LinkedHashMap<>();
        for (Reserva reserva : cobradas) {
            porFuncion.computeIfAbsent(reserva.getFuncionId(), id -> new ArrayList<>()).add(reserva);
        }

        List<DeclaracionJurada.FilaFuncion> filas = new ArrayList<>();
        Map<Integer, DeclaracionJurada.TotalPelicula> porPelicula = new HashMap<>();
        DeclaracionJurada.Totales total = DeclaracionJurada.Totales.CERO;
        for (List<Reserva> reservas : porFuncion.values()) {
            Funcion funcion = reservas.get(0).getFuncion();
            Pelicula pelicula = funcion.getPelicula();
            Bordero bordero = bordero(funcion, pelicula.getTitulo(), funcion.getSala().getNombre(),
                    reservas, pagos);
            filas.add(new DeclaracionJurada.FilaFuncion(bordero, funcion.getVersion(),
                    funcion.getProyeccion(), pelicula.getClasificacion()));
            DeclaracionJurada.TotalPelicula acumulado = porPelicula.getOrDefault(pelicula.getId(),
                    new DeclaracionJurada.TotalPelicula(pelicula.getTitulo(), pelicula.getClasificacion(),
                            DeclaracionJurada.Totales.CERO));
            porPelicula.put(pelicula.getId(), new DeclaracionJurada.TotalPelicula(acumulado.titulo(),
                    acumulado.clasificacion(), acumulado.totales().mas(bordero)));
            total = total.mas(bordero);
        }

        List<DeclaracionJurada.TotalPelicula> peliculas = porPelicula.values().stream()
                .sorted(Comparator.comparing(DeclaracionJurada.TotalPelicula::titulo))
                .toList();
        return new DeclaracionJurada(desde, hasta, reloj.ahora(), filas, peliculas, total);
    }

    // Se declara lo cobrado: una reserva sin pagar retiene butacas pero no vendió.
    private Bordero bordero(Funcion funcion, String pelicula, String sala, List<Reserva> reservas,
                            Map<Integer, Pago> pagosPorReserva) {
        Map<TipoTarifa, Bordero.TotalPorTarifa> porTarifa = new EnumMap<>(TipoTarifa.class);
        int espectadores = 0;
        Dinero bruta = Dinero.CERO;
        Dinero descuentos = Dinero.CERO;
        Dinero neta = Dinero.CERO;

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
            // Desglose a precio de lista; totales con el pago, único que sabe cuánto sacó la promo.
            bruta = bruta.mas(pago.getSubtotal());
            descuentos = descuentos.mas(pago.getDescuento());
            neta = neta.mas(pago.getMonto());
        }

        return new Bordero(funcion.getId(), pelicula, sala, funcion.getInicio(),
                reloj.ahora(), espectadores,
                bruta, descuentos, neta, porTarifa);
    }

    private Map<Integer, Pago> pagosPorReserva(List<Reserva> reservas) {
        Map<Integer, Pago> pagos = new HashMap<>();
        for (Pago cobro : pagoRepository.findByReservaIdIn(reservas.stream().map(Reserva::getId).toList())) {
            pagos.put(cobro.getReservaId(), cobro);
        }
        return pagos;
    }

    public Bordero exportarBordero(int funcionId) {
        Bordero bordero = borderoDe(funcionId);
        generadorBordero.emitir(bordero);
        LOG.info("bordero funcion {} · {} espectadores · bruto {} · neto {}",
                funcionId, bordero.espectadores(), bordero.recaudacionBruta(),
                bordero.recaudacionNeta());
        return bordero;
    }

    // Solo el candy con reservaId: el de mostrador va al arqueo (GestorCaja#totalCandyDe).
    public InformeFuncion informeDe(int funcionId) {
        Bordero bordero = borderoDe(funcionId);

        List<Integer> reservas = reservaRepository.findByFuncion_Id(funcionId).stream()
                .map(Reserva::getId).toList();
        List<CompraCandy> compras = compraCandyRepository.findByReservaIdIn(reservas);
        Dinero candy = Dinero.sumar(compras.stream().map(CompraCandy::getTotal).toList());

        return new InformeFuncion(bordero, compras.size(), candy, bordero.recaudacionNeta().mas(candy));
    }

    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + funcionId));
    }
}
