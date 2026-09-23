package ar.uade.cine.infrastructure.pasarelas;

import java.util.Optional;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

public interface PasarelaPagos {

    Checkout crear(int reservaId, MedioPago medio, Dinero monto);

    Optional<Checkout> buscar(String checkoutId);

    String autorizar(Checkout checkout);

    // Guarda reservaId y medio para que quien confirma no pueda decir que paga otra cosa.
    record Checkout(String id, int reservaId, MedioPago medio, Dinero monto,
                    String urlPago, String codigoQr) {
    }
}
