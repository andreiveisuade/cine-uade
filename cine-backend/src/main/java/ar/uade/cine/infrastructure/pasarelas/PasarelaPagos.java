package ar.uade.cine.infrastructure.pasarelas;

import java.time.LocalDateTime;
import java.util.Optional;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

// Puerto: la pasarela de pagos electrónicos (checkout y autorización); cambiarla no toca GestorPagos (DIP).
public interface PasarelaPagos {

    // Vence con la reserva (R17): después ya soltó sus butacas y lo cobrado no tendría devolución (R13).
    Checkout crear(int reservaId, MedioPago medio, Dinero monto, LocalDateTime vence);

    Optional<Checkout> buscar(String checkoutId);

    String autorizar(Checkout checkout);

    // Guarda reservaId y medio para que quien confirma no pueda decir que paga otra cosa.
    record Checkout(String id, int reservaId, MedioPago medio, Dinero monto,
                    String urlPago, String codigoQr) {
    }
}
