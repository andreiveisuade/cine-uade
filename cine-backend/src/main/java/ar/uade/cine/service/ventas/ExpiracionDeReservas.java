package ar.uade.cine.service.ventas;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.candy.GestorCandy;

// R17: vence las reservas que no se pagaron a tiempo, para que sus butacas vuelvan a la venta.
// Alta cohesión (GRASP) / SRP: salió de Ocupacion; quien consulta la ocupación la llama (sin scheduler).
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiracionDeReservas {

    private final ReservaRepository reservaRepository;
    private final GestorCandy candy;
    private final Reloj reloj;

    // Escribe porque el UNIQUE no sabe de vencimientos.
    @Transactional
    public void expirarVencidas(List<Reserva> reservas) {
        LocalDateTime ahora = reloj.ahora();
        for (Reserva reserva : reservas) {
            if (reserva.estaVencida(ahora)) {
                reserva.expirar();
                reservaRepository.save(reserva);
                candy.anularPendiente(reserva.getId());
                log.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
