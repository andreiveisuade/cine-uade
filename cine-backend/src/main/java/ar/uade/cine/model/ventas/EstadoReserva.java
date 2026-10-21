package ar.uade.cine.model.ventas;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Estados de una reserva y sus transiciones (R5, R13, R17, R18); patrón State con una constante por estado.
// Patrón State (GoF): la regla de qué se puede hacer en cada estado vive en la constante, no en Reserva.
// Cada constante sobrescribe lo que permite; el resto rechaza con «La reserva está <etiqueta>: ...». La etiqueta es para mensajes.
public enum EstadoReserva {

    RESERVADA("sin pagar") {
        @Override
        EstadoReserva pagar() {
            return PAGADA;
        }

        @Override
        EstadoReserva cancelar() {
            return CANCELADA;
        }

        @Override
        EstadoReserva expirar() {
            return EXPIRADA;
        }

        @Override
        public boolean esperaPago() {
            return true;
        }

        @Override
        public boolean ocupaButacas() {
            return true;
        }
    },

    PAGADA("pagada") {
        @Override
        EstadoReserva ingresar() {
            return this;
        }

        @Override
        public boolean ocupaButacas() {
            return true;
        }

        @Override
        public boolean estaPagada() {
            return true;
        }
    },

    CANCELADA("cancelada"),
    EXPIRADA("vencida");

    static final String NO_SE_PUEDE_COBRAR = "no se puede cobrar";

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    // De paquete: solo Reserva cambia su estado.
    EstadoReserva pagar() {
        throw rechazo(NO_SE_PUEDE_COBRAR);
    }

    // R13: se cancela solo lo que todavía no se cobró.
    EstadoReserva cancelar() {
        throw rechazo("solo se puede cancelar una reserva sin cobrar");
    }

    // R17: vence solo la que sigue esperando el pago.
    EstadoReserva expirar() {
        throw rechazo("no puede expirar");
    }

    // R18: a la sala se entra con una reserva pagada.
    EstadoReserva ingresar() {
        throw rechazo("solo se ingresa con una reserva pagada");
    }

    // Todavía se puede cobrar o cancelar, y todavía puede vencer.
    public boolean esperaPago() {
        return false;
    }

    // R4: la butaca de una reserva que sigue en pie no se vende a otro.
    public boolean ocupaButacas() {
        return false;
    }

    public boolean estaPagada() {
        return false;
    }

    String porQueNo(String queNoSePuede) {
        return "La reserva está " + etiqueta + ": " + queNoSePuede;
    }

    private DatoInvalido rechazo(String queNoSePuede) {
        return new DatoInvalido(porQueNo(queNoSePuede));
    }
}
