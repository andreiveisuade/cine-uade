package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.PagoRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.model.dinero.Dinero;

/**
 * El cierre de caja de un día: cuánto entró, por qué medio, y por cuál de las dos cajas
 * —boletería y candy—. Es el corte <strong>por día</strong>, al lado de
 * {@link GestorInformes} que corta por función.
 *
 * <p>Antes el arqueo vivía en los gestores que cobran, y "cuánto entró hoy" obligaba a
 * preguntarle a dos y sumar afuera. Eran dos razones de cambio en la misma clase: cobrar
 * cambia cuando cambia cómo se cobra; el arqueo, cuando cambia qué se declara. Solo lee.
 */
@Service
public class GestorCaja {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final CompraCandyRepository compraCandyRepository;

    public GestorCaja(PagoRepository pagoRepository, ReservaRepository reservaRepository, CompraCandyRepository compraCandyRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.compraCandyRepository = compraCandyRepository;
    }

    /** El cierre de la boletería del día: total, entradas vendidas y reparto por medio de pago. */
    public Arqueo arqueoDe(LocalDate fecha) {
        List<Pago> delDia = pagoRepository.findByDia(fecha);
        // El pago no guarda cuántas butacas se llevó —sería el dato en dos lados—; se
        // traen las reservas de una vez y no una consulta por pago.
        Map<Integer, Integer> entradasPorReserva = reservaRepository
                .findAllById(delDia.stream().map(Pago::getReservaId).toList()).stream()
                .collect(Collectors.toMap(Reserva::getId, Reserva::getCantidadEntradas));

        Map<MedioPago, Arqueo.TotalPorMedio> porMedio = new EnumMap<>(MedioPago.class);
        Dinero total = Dinero.CERO;
        int entradas = 0;
        for (Pago pago : delDia) {
            Arqueo.TotalPorMedio acumulado = porMedio.getOrDefault(pago.getMedio(),
                    new Arqueo.TotalPorMedio(0, Dinero.CERO));
            porMedio.put(pago.getMedio(), new Arqueo.TotalPorMedio(acumulado.cantidad() + 1,
                    acumulado.total().mas(pago.getMonto())));
            total = total.mas(pago.getMonto());
            entradas += entradasPorReserva.getOrDefault(pago.getReservaId(), 0);
        }
        return new Arqueo(fecha, total, entradas, porMedio, delDia);
    }

    /**
     * Cuánto entró por el candy en el día, mostrador incluido. Aparte del arqueo de
     * boletería: el borderó del INCAA solo mira entradas, y sumarlos obligaría a separarlos.
     */
    public Dinero totalCandyDe(LocalDate fecha) {
        return Dinero.sumar(compraCandyRepository.findByDia(fecha).stream()
                .map(CompraCandy::getTotal).toList());
    }

}
