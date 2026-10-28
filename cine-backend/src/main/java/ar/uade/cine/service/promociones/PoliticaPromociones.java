package ar.uade.cine.service.promociones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

// Interfaz: el descuento que corresponde a un cobro; GestorPagos depende de la regla y no del gestor (DIP).
public interface PoliticaPromociones {

    Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion, MedioPago medio);

    // El nombre es para mostrar la promo antes de cobrar (la cotización); el pago guarda solo el id.
    record Descuento(Integer promocionId, String promocion, Dinero monto) {

        private static final Descuento NINGUNO = new Descuento(null, null, Dinero.CERO);

        public static Descuento ninguno() {
            return NINGUNO;
        }
    }
}
