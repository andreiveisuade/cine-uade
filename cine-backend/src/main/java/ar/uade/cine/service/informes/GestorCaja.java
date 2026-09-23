package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.PagoRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.model.dinero.Dinero;

@Service
@Transactional(readOnly = true)
public class GestorCaja {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final CompraCandyRepository compraCandyRepository;

    public GestorCaja(PagoRepository pagoRepository, ReservaRepository reservaRepository, CompraCandyRepository compraCandyRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.compraCandyRepository = compraCandyRepository;
    }

    public Arqueo arqueoDe(LocalDate fecha) {
        List<Pago> delDia = pagoRepository.findByDia(fecha);
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

    public Dinero totalCandyDe(LocalDate fecha) {
        return Dinero.sumar(compraCandyRepository.findByDia(fecha).stream()
                .map(CompraCandy::getTotal).toList());
    }

}
