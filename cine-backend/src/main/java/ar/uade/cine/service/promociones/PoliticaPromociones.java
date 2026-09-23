package ar.uade.cine.service.promociones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;

public interface PoliticaPromociones {

    Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion, MedioPago medio);

    record Descuento(Integer promocionId, Dinero monto) {

        private static final Descuento NINGUNO = new Descuento(null, Dinero.CERO);

        public static Descuento ninguno() {
            return NINGUNO;
        }
    }
}
