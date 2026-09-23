package ar.uade.cine.service.ventas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.service.RecursoNoEncontrado;

@Service
@Transactional
public class GestorAcceso {

    private static final Logger LOG = LoggerFactory.getLogger(GestorAcceso.class);

    private final ReservaRepository reservaRepository;
    private final Reloj reloj;

    public GestorAcceso(ReservaRepository reservaRepository, Reloj reloj) {
        this.reservaRepository = reservaRepository;
        this.reloj = reloj;
    }

    public Reserva registrarIngreso(String codigo) {
        Reserva reserva = reservaRepository.findByCodigo(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontrado("No existe ninguna reserva con ese código"));
        reserva.registrarIngreso(reloj.ahora());
        reservaRepository.save(reserva);
        LOG.info("ingreso reserva {} · codigo {} · {} personas",
                reserva.getId(), reserva.getCodigo(), reserva.getCantidadEntradas());
        return reserva;
    }
}
