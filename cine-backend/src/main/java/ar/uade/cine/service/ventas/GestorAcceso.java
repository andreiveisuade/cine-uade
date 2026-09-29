package ar.uade.cine.service.ventas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.RecursoNoEncontrado;

// Ingreso a la sala por código de acceso (R18); coordina y la reserva decide si puede entrar.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorAcceso {

    private final ReservaRepository reservaRepository;
    private final Reloj reloj;

    public Reserva registrarIngreso(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Falta el código de acceso");
        }
        Reserva reserva = reservaRepository.findByCodigo(Reserva.normalizarCodigo(codigo))
                .orElseThrow(() -> new RecursoNoEncontrado("No existe ninguna reserva con ese código"));
        reserva.registrarIngreso(reloj.ahora());
        // Sin el código: es la única credencial del cliente, y con el id alcanza para rastrearlo.
        log.info("ingreso reserva {} · {} personas", reserva.getId(), reserva.getCantidadEntradas());
        return reserva;
    }
}
