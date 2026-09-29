package ar.uade.cine.service.ventas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.promociones.PoliticaPromociones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.RecursoNoEncontrado;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorPagos {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final PoliticaPromociones promociones;
    private final PasarelaPagos pasarela;
    private final GeneradorRecibo generadorRecibo;
    private final Reloj reloj;

    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        String autorizacion = medio.autorizacion(codigoAutorizacion);

        // Recién acá se conoce el medio, y de él dependen las promociones.
        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);

        Pago pago = new Pago(reservaId, reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, reloj.ahora(), autorizacion);
        pagoRepository.save(pago);

        reserva.pagar();
        reservaRepository.save(reserva);
        emitirRecibo(pago, reserva);
        log.info("pago reserva {} · {} · subtotal {}{} · cobrado {}",
                reservaId, medio, reserva.getTotal(),
                !descuento.monto().esCero()
                        ? " · promo " + descuento.promocionId() + " -" + descuento.monto()
                        : " · sin promo",
                pago.getMonto());

        return pago;
    }

    // Valida como al cobrar: mandar a pagar algo incobrable terminaría en una devolución, que no existe (R13).
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        if (!medio.requiereAutorizacion()) {
            throw new IllegalArgumentException("El pago con " + medio
                    + " se cobra en la caja del cine, no por checkout");
        }

        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);
        Dinero monto = reserva.getTotal().menos(descuento.monto());

        PasarelaPagos.Checkout checkout = pasarela.crear(reservaId, medio, monto);
        log.info("checkout {} · reserva {} · {} · a pagar {}",
                checkout.id(), reservaId, medio, monto);
        return checkout;
    }

    // La reserva sale del checkout y no de quien confirma. El descuento se recalcula: pudo cambiar una promoción.
    public Pago confirmarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = pasarela.buscar(checkoutId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el checkout " + checkoutId));

        String codigoAutorizacion = pasarela.autorizar(checkout);
        return cobrar(checkout.reservaId(), checkout.medio(), codigoAutorizacion);
    }

    private Reserva buscarReserva(int reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la reserva " + reservaId));
    }

    // Lo propio de la reserva lo decide ella (el mismo método que habilita el cobro en la vista);
    // acá queda lo que viene del pedido y lo que necesita la base.
    private Funcion validarQueSePuedaCobrar(Reserva reserva, MedioPago medio) {
        // R17: puede figurar RESERVADA si nadie consultó la función desde que venció.
        reserva.impedimentoParaCobrar(reloj.ahora()).ifPresent(motivo -> {
            throw new IllegalArgumentException(motivo);
        });
        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        if (pagoRepository.existsByReservaId(reserva.getId())) {
            throw new IllegalArgumentException("La reserva " + reserva.getId() + " ya tiene un pago registrado");
        }
        return reserva.getFuncion();
    }

    private void emitirRecibo(Pago pago, Reserva reserva) {
        if (!pago.getMedio().requiereAutorizacion()) {
            generadorRecibo.emitir(pago, reserva);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Pago> buscarPorReserva(int reservaId) {
        return pagoRepository.findByReservaId(reservaId);
    }

    @Transactional(readOnly = true)
    public List<Pago> buscarPorReservas(Collection<Integer> reservaIds) {
        return pagoRepository.findByReservaIdIn(reservaIds);
    }

}
