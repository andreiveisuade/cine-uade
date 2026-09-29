package ar.uade.cine.model.ventas;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Estados de una reserva y sus transiciones (R5, R13, R17, R18); patrón State con una constante por estado.
// Patrón State (GoF): el comportamiento que depende del estado vive en el estado y no en su dueño.
// Qué resolvía: Reserva preguntaba `estado == RESERVADA` o `estado != PAGADA` en cada operación, y la regla
// de qué se puede hacer en cada estado estaba repartida en cinco métodos. Sumar un estado era revisarlos todos.
// Cómo se lee: cada constante sobrescribe solo lo que ella permite. RESERVADA se puede pagar, cancelar o
// expirar; PAGADA deja ingresar. Lo que una constante no sobrescribe cae en el método de abajo, que
// rechaza con «La reserva está <etiqueta>: <lo que no se puede>». Una transición devuelve el estado
// siguiente y Reserva lo asigna: `estado = estado.pagar()`.
// La etiqueta es para los mensajes ("La reserva está vencida"); en el JSON y en la base va name().
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

    // Pagar y entrar no la sacan de PAGADA: el ingreso queda en Reserva.ingresadaEn.
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

    // Las transiciones son de paquete: solo Reserva cambia su estado, y lo hace con un método con intención.
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

    // El texto de todos los rechazos por estado; Reserva lo usa también para decir por qué no se cobra.
    String porQueNo(String queNoSePuede) {
        return "La reserva está " + etiqueta + ": " + queNoSePuede;
    }

    private DatoInvalido rechazo(String queNoSePuede) {
        return new DatoInvalido(porQueNo(queNoSePuede));
    }
}
