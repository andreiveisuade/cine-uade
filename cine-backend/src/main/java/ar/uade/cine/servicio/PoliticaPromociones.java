package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;

/**
 * Qué descuento le corresponde a una compra. Es lo único que necesita saber quien cobra:
 * un monto y de qué promoción salió.
 *
 * <p>Existe para que {@link GestorPagos} no dependa de {@link GestorPromociones}. Era el
 * único lugar del sistema donde un gestor estaba atado a otro gestor concreto, y no por
 * necesidad: cobrar no necesita el ABM de promociones, ni saber que se elige la que más
 * descuenta. Necesita el número.
 *
 * <p>Con el contrato de por medio, cambiar cómo se decide el descuento —acumular varias,
 * traerlo de un servicio externo, apagarlo— es otra implementación de esta interfaz, y
 * GestorPagos no se entera.
 */
public interface PoliticaPromociones {

    /**
     * El descuento que corre para esas entradas en esa función, pagando con ese medio.
     *
     * @param entradas las de la reserva completa; qué entradas participan del descuento es
     *                 parte de la política y se resuelve adentro
     * @param inicioFuncion cuándo empieza la función, que es contra lo que se evalúa la
     *                      vigencia y no contra el momento de la compra
     */
    Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion, MedioPago medio);

    /**
     * Cuánto se descuenta y por qué promoción.
     *
     * @param promocionId {@code null} cuando no corrió ninguna, que es lo que se guarda en
     *                    el pago para poder explicar después por qué se cobró eso
     */
    record Descuento(Integer promocionId, double monto) {

        private static final Descuento NINGUNO = new Descuento(null, 0);

        /** No corría ninguna promoción: se cobra el precio de lista. */
        public static Descuento ninguno() {
            return NINGUNO;
        }
    }
}
