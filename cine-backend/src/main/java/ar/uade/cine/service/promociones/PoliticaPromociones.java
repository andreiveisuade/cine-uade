package ar.uade.cine.service.promociones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

/**
 * El descuento que le corresponde a una compra. Existe para que {@code GestorPagos} no
 * dependa de {@link GestorPromociones}: cobrar necesita el monto, no el ABM.
 */
public interface PoliticaPromociones {

    /**
     * @param entradas las de la reserva completa; cuáles participan lo decide la política
     * @param inicioFuncion la vigencia se evalúa contra la función, no contra la compra
     */
    Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion, MedioPago medio);

    /**
     * @param promocionId {@code null} si no corrió ninguna; se guarda en el pago para
     *                    explicar el cobro
     */
    record Descuento(Integer promocionId, Dinero monto) {

        private static final Descuento NINGUNO = new Descuento(null, Dinero.CERO);

        public static Descuento ninguno() {
            return NINGUNO;
        }
    }
}
