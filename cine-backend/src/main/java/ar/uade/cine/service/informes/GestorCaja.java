package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Corte de caja por día de cobro, de boletería y de candy; lee y deja la suma a Arqueo y ArqueoCandy.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GestorCaja {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final CompraCandyRepository compraCandyRepository;

    public Arqueo arqueoDe(LocalDate fecha) {
        List<Pago> pagos = pagoRepository.findByDia(fecha);
        List<Reserva> reservas = reservaRepository.findAllById(pagos.stream().map(Pago::getReservaId).toList());
        return Arqueo.de(fecha, pagos, reservas);
    }

    public ArqueoCandy arqueoCandyDe(LocalDate fecha) {
        return ArqueoCandy.de(fecha, compraCandyRepository.findByDia(fecha));
    }
}
