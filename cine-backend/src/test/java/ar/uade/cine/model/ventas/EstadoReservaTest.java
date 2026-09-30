package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class EstadoReservaTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la reservada se cobra,          RESERVADA, pagar,    PAGADA
            la reservada se cancela,        RESERVADA, cancelar, CANCELADA
            la reservada vence,             RESERVADA, expirar,  EXPIRADA
            con la pagada se entra y sigue, PAGADA,    ingresar, PAGADA
            """)
    void cadaEstadoDecideAQueEstadoPasa(String caso, EstadoReserva desde, String transicion,
                                        EstadoReserva hacia) {
        assertEquals(hacia, aplicar(desde, transicion));
    }

    // El texto es el de antes de que el estado decidiera: lo leen la boletería y la puerta.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la pagada no se cobra dos veces, PAGADA,    pagar,    La reserva está pagada: no se puede cobrar
            la cancelada no se cobra,        CANCELADA, pagar,    La reserva está cancelada: no se puede cobrar
            la vencida no se cobra,          EXPIRADA,  pagar,    La reserva está vencida: no se puede cobrar
            la pagada no se cancela,         PAGADA,    cancelar, La reserva está pagada: solo se puede cancelar una reserva sin cobrar
            la cancelada no se recancela,    CANCELADA, cancelar, La reserva está cancelada: solo se puede cancelar una reserva sin cobrar
            la vencida no se cancela,        EXPIRADA,  cancelar, La reserva está vencida: solo se puede cancelar una reserva sin cobrar
            la pagada no vence,              PAGADA,    expirar,  La reserva está pagada: no puede expirar
            la cancelada no vence,           CANCELADA, expirar,  La reserva está cancelada: no puede expirar
            la vencida no vuelve a vencer,   EXPIRADA,  expirar,  La reserva está vencida: no puede expirar
            sin pagar no se entra,           RESERVADA, ingresar, La reserva está sin pagar: solo se ingresa con una reserva pagada
            con la cancelada no se entra,    CANCELADA, ingresar, La reserva está cancelada: solo se ingresa con una reserva pagada
            con la vencida no se entra,      EXPIRADA,  ingresar, La reserva está vencida: solo se ingresa con una reserva pagada
            """)
    void loQueUnEstadoNoPermiteSeRechazaDiciendoEnQueEstadoEsta(String caso, EstadoReserva desde,
                                                               String transicion, String mensaje) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> aplicar(desde, transicion));

        assertEquals(mensaje, error.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la reservada espera el pago y retiene sus butacas, RESERVADA, true,  true,  false
            la pagada retiene sus butacas,                     PAGADA,    false, true,  true
            la cancelada ya las soltó,                         CANCELADA, false, false, false
            la vencida ya las soltó,                           EXPIRADA,  false, false, false
            """)
    void cadaEstadoContestaLoQueSeLePregunta(String caso, EstadoReserva estado, boolean esperaPago,
                                             boolean ocupaButacas, boolean estaPagada) {
        assertEquals(esperaPago, estado.esperaPago());
        assertEquals(ocupaButacas, estado.ocupaButacas());
        assertEquals(estaPagada, estado.estaPagada());
    }

    private static EstadoReserva aplicar(EstadoReserva estado, String transicion) {
        return switch (transicion) {
            case "pagar" -> estado.pagar();
            case "cancelar" -> estado.cancelar();
            case "expirar" -> estado.expirar();
            case "ingresar" -> estado.ingresar();
            default -> throw new AssertionError("Transición desconocida: " + transicion);
        };
    }
}
