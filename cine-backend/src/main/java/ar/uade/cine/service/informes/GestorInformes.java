package ar.uade.cine.service.informes;

import java.util.EnumMap;
import java.util.HashMap;
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

    // Se declara lo cobrado: una reserva sin pagar retiene butacas pero no vendió.
    public Bordero borderoDe(int funcionId) {
        Funcion funcion = buscarFuncion(funcionId);
        Pelicula pelicula = peliculaRepository.findById(funcion.getPeliculaId())
                .orElseThrow(() -> new RecursoNoEncontrado(
                        "No existe la película " + funcion.getPeliculaId()));
        Sala sala = salaRepository.findById(funcion.getSalaId())
                .orElseThrow(() -> new RecursoNoEncontrado(
                        "No existe la sala " + funcion.getSalaId()));

        Map<TipoTarifa, Bordero.TotalPorTarifa> porTarifa = new EnumMap<>(TipoTarifa.class);
        int espectadores = 0;
        Dinero bruta = Dinero.CERO;
        Dinero descuentos = Dinero.CERO;
        Dinero neta = Dinero.CERO;
        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
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
            // Desglose a precio de lista; totales con el pago, único que sabe cuánto sacó la promo.
            bruta = bruta.mas(pago.getSubtotal());
            descuentos = descuentos.mas(pago.getDescuento());
            neta = neta.mas(pago.getMonto());
        }

        return new Bordero(funcionId, pelicula.getTitulo(), sala.getNombre(), funcion.getInicio(),
                reloj.ahora(), espectadores,
                bruta, descuentos, neta, porTarifa);
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
