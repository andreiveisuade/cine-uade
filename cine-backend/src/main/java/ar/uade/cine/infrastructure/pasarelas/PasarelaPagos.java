package ar.uade.cine.infrastructure.pasarelas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

// Puerto: la pasarela de pagos electrónicos (checkout y autorización); cambiarla no toca GestorPagos (DIP).
public interface PasarelaPagos {

    // Vence con la reserva (R17): después ya soltó sus butacas y lo cobrado no tendría devolución (R13).
    // Entradas (ya con el descuento) y candy por separado, para que el comprador vea los dos (R30).
    Checkout crear(int reservaId, MedioPago medio, Dinero entradas, Dinero candy, LocalDateTime vence);

    Optional<Checkout> buscar(String checkoutId);

    String autorizar(Checkout checkout);

    // Los checkouts con un pago aprobado desde ese momento, para cobrarlos aunque el comprador no vuelva.
    List<String> aprobadosDesde(LocalDateTime desde);

    // Guarda reservaId y medio para que quien confirma no pueda decir que paga otra cosa.
    record Checkout(String id, int reservaId, MedioPago medio, Dinero monto,
                    String urlPago, String codigoQr) {
    }
}
