package ar.uade.cine.pasarelas;

import java.util.Optional;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Quien autoriza un pago electrónico, que nunca es el cine. El cobro en la web no termina
 * cuando el cajero aprieta un botón: se abre un checkout, el cliente paga contra un
 * tercero —MercadoPago, el posnet, el banco— y ese tercero devuelve un código de
 * autorización que es la prueba de que la plata existe. R11 pide ese código justamente
 * porque el cine no se lo puede inventar.
 *
 * <p>Es una interfaz por el mismo motivo que {@link ar.uade.cine.comprobantes.GeneradorTicket}:
 * hoy atrás hay una emulación —no hay credenciales ni llamadas de red en este TP— y mañana
 * podría haber una integración de verdad. Con el contrato de por medio, eso es cambiar un
 * argumento en {@code Aplicacion} y {@link ar.uade.cine.servicio.ventas.GestorPagos} no se entera.
 *
 * <p>Acá no vive ninguna regla: no decide si la reserva se puede cobrar, ni si el medio
 * exige autorización, ni si la función ya empezó. Eso es del gestor. La pasarela solo crea
 * el checkout, lo recuerda y autoriza.
 */
public interface PasarelaPagos {

    /**
     * Abre el checkout: lo que el cliente ve como un QR o un link para pagar.
     *
     * @param monto lo que se va a cobrar, con el descuento ya resuelto. El checkout tiene
     *              que mostrar el importe final o el cliente aprobaría un número y pagaría
     *              otro
     */
    Checkout crear(int reservaId, MedioPago medio, Dinero monto);

    /** Vacío si ese checkout no existe. Quién avisa del error es el gestor, no la pasarela. */
    Optional<Checkout> buscar(String checkoutId);

    /**
     * El pago llegó: devuelve el código de autorización del procesador, que es lo que se
     * guarda en el {@link ar.uade.cine.dominio.ventas.Pago} y lo único que después permite
     * reclamarle algo a la pasarela.
     */
    String autorizar(Checkout checkout);

    /**
     * Una intención de pago abierta. Guarda {@code reservaId} y {@code medio} porque el
     * checkout es lo único que llega de vuelta cuando el cliente termina de pagar: sin
     * ellos, quien confirma tendría que volver a decir qué está pagando, y podría decir
     * otra cosa.
     *
     * @param urlPago dónde pagaría el cliente
     * @param codigoQr lo que codifica el QR. El dibujo es problema de quien lo muestra:
     *                 acá viaja el contenido, igual que los enums viajan por nombre
     */
    record Checkout(String id, int reservaId, MedioPago medio, Dinero monto,
                    String urlPago, String codigoQr) {
    }
}
