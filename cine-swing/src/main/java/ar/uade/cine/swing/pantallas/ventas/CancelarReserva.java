package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Cancelar la reserva elegida y liberar sus butacas. ConcreteCommand de AccionDeFila; Receiver: ApiVentas.
record CancelarReserva(ApiVentas api) implements AccionDeFila<Reserva> {

    @Override
    public boolean destructiva() {
        return true;
    }

    @Override
    public String texto() {
        return "Cancelar reserva";
    }

    // Lo decide el backend con las mismas reglas que al cancelar (R13, R17, R19): habilitado no termina en un rechazo.
    @Override
    public boolean habilitada(Reserva r) {
        return r.cancelable();
    }

    // El no es "Volver": "Cancelar" se confundiría con la acción misma.
    @Override
    public Optional<Pregunta> pregunta(Reserva r) {
        return Optional.of(new Pregunta("¿Cancelar la reserva #" + r.id() + "? Las butacas quedan libres.",
                "Sí, cancelar la reserva", "Volver"));
    }

    @Override
    public void ejecutar(Reserva r) {
        api.cancelarReserva(r.id());
    }

    @Override
    public String hecho(Reserva r) {
        return "Reserva cancelada, las butacas quedaron libres";
    }
}
