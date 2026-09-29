package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;

class ReservaTest {

    private static final Sala SALA = new Sala("Sala 1", TipoSala.DOS_D, 15);
    private static final Funcion FUNCION = new Funcion(
            new Pelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13), SALA,
            LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
    private static final Cliente CLIENTE = new Cliente("Ana", "ana@mail.com");
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 8, 14, 10, 0);

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin función,          funcion,  Falta la función
            sin cliente,          cliente,  Falta el cliente
            sin fecha y hora,     creadaEn, Falta la fecha y hora de la reserva
            sin butacas,          entradas, Hay que elegir al menos una butaca
            """)
    void loQueFaltaSeRechazaDiciendoQueFalta(String caso, String falta, String mensaje) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new Reserva(
                falta.equals("funcion") ? null : FUNCION,
                falta.equals("cliente") ? null : CLIENTE,
                falta.equals("entradas") ? List.of() : entradas("A1"),
                falta.equals("creadaEn") ? null : CREADA));

        assertEquals(mensaje, error.getMessage());
    }

    @Test
    void unaCompraLlevaHastaDiezButacas() {
        assertDoesNotThrow(() -> new Reserva(FUNCION, CLIENTE, primeras(10), CREADA));

        DatoInvalido once = assertThrows(DatoInvalido.class,
                () -> new Reserva(FUNCION, CLIENTE, primeras(11), CREADA));

        assertEquals("Una compra tiene que tener como máximo 10 butacas", once.getMessage());
    }

    // El bloqueo aparta butacas antes de que la reserva exista, y con el mismo tope.
    @Test
    void elTopeTambienSeAplicaSinReserva() {
        assertDoesNotThrow(() -> Reserva.validarTopeDeButacas(Collections.nCopies(10, "A1")));

        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> Reserva.validarTopeDeButacas(Collections.nCopies(11, "A1")));

        assertEquals("Una compra tiene que tener como máximo 10 butacas", error.getMessage());
    }

    @Test
    void laMismaButacaDosVecesSeRechazaComoRepetida() {
        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Reserva(FUNCION, CLIENTE, entradas("A1", "B2", "A1"), CREADA));

        assertEquals("La butaca A1 está repetida en el pedido", error.getMessage());
    }

    @Test
    void nacePorPagarConUnCodigoDeAccesoYSusButacasOcupadas() {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, entradas("A1", "A2"), CREADA);

        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
        assertEquals(8, reserva.getCodigo().length());
        assertEquals(Dinero.de(10000), reserva.getTotal());
        assertTrue(reserva.estaVigente());
    }

    // La función es el 20/08 a las 20: el acomodador deja pasar ese día y no otro.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            a primera hora,          2026-08-20T00:00
            antes de empezar,        2026-08-20T19:45
            con la función empezada, 2026-08-20T20:15
            """)
    void elDiaDeLaFuncionSeEntra(String caso, LocalDateTime cuando) {
        Reserva reserva = pagada();

        reserva.registrarIngreso(cuando);

        assertEquals(cuando, reserva.getIngresadaEn());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la víspera,     2026-08-19T23:59
            el día después, 2026-08-21T00:00
            """)
    void otroDiaNoSeEntraNiSeGastaLaEntrada(String caso, LocalDateTime cuando) {
        Reserva reserva = pagada();

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> reserva.registrarIngreso(cuando));

        assertEquals("La función es el 20/08: se entra solo ese día", error.getMessage());
        assertNull(reserva.getIngresadaEn());
    }

    private static Reserva pagada() {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, entradas("A1"), CREADA);
        reserva.pagar();
        return reserva;
    }

    private static List<Entrada> entradas(String... codigos) {
        List<Entrada> entradas = new ArrayList<>();
        for (String codigo : codigos) {
            int fila = codigo.charAt(0) - 'A' + 1;
            int numero = Integer.parseInt(codigo.substring(1));
            entradas.add(new Entrada(new Asiento(SALA, fila, numero, TipoAsiento.ESTANDAR), TipoTarifa.GENERAL,
                    Dinero.de(5000)));
        }
        return entradas;
    }

    private static List<Entrada> primeras(int cuantas) {
        List<Entrada> entradas = new ArrayList<>();
        for (int numero = 1; numero <= cuantas; numero++) {
            entradas.addAll(entradas("A" + numero));
        }
        return entradas;
    }
}
