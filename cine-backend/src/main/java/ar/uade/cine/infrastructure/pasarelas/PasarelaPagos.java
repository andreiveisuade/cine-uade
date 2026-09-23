package ar.uade.cine.infrastructure.pasarelas;

import java.util.Optional;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

/**
 * El tercero que autoriza un pago electrónico y devuelve el código que pide R11. Hoy es
 * una emulación; se elige en {@code Adaptadores}. Sin reglas: eso es de
 * {@link ar.uade.cine.service.ventas.GestorPagos}.
 */
public interface PasarelaPagos {

    /** @param monto con el descuento ya resuelto: el cliente tiene que aprobar el importe final */
    Checkout crear(int reservaId, MedioPago medio, Dinero monto);

    Optional<Checkout> buscar(String checkoutId);

    /** El código de autorización que queda en el {@link ar.uade.cine.model.ventas.Pago}. */
    String autorizar(Checkout checkout);

    /**
     * Guarda {@code reservaId} y {@code medio} para que quien confirma no pueda decir que
     * paga otra cosa.
     *
     * @param codigoQr el contenido del QR; el dibujo lo hace quien lo muestra
     */
    record Checkout(String id, int reservaId, MedioPago medio, Dinero monto,
                    String urlPago, String codigoQr) {
    }
}
