package ar.uade.cine.service.ventas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.ventas.CodigoDeAcceso;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Ingreso a la sala por código de acceso (R18); coordina y la reserva decide si puede entrar.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorAcceso {

    private final ReservaRepository reservaRepository;
    private final Reloj reloj;

    // Un código que falta lo rechaza PedidoAccesoDTO; sin HTTP, uno vacío no encuentra reserva (404).
    public Reserva registrarIngreso(String codigo) {
        Reserva reserva = reservaRepository.findByCodigo(new CodigoDeAcceso(codigo).valor())
                .orElseThrow(() -> new RecursoNoEncontrado("No existe ninguna reserva con ese código"));
        reserva.registrarIngreso(reloj.ahora());
        // Sin el código: es la única credencial del cliente, y con el id alcanza para rastrearlo.
        log.info("ingreso reserva {} · {} personas", reserva.getId(), reserva.getCantidadEntradas());
        return reserva;
    }
}
