package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
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
            LocalDateTime.of(2026, 8, 20, 20, 0),
            new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
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
                falta.equals("entradas") ? List.of() : butacas("A1"),
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
                () -> new Reserva(FUNCION, CLIENTE, butacas("A1", "B2", "A1"), CREADA));

        assertEquals("La butaca A1 está repetida en el pedido", error.getMessage());
    }

    // R34: se sientan juntos. Se mira el pedido, no el orden en que se tocaron las butacas.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            una sola,           A5
            corridas,           A3 A4 A5
            desordenadas,       A5 A3 A4
            """)
    void lasButacasContiguasDeUnaFilaSeReservan(String caso, String codigos) {
        assertDoesNotThrow(() -> new Reserva(FUNCION, CLIENTE, butacas(codigos.split(" ")), CREADA));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            con un hueco,       A1 A3
            en filas pegadas,   A1 B1
            una en otra fila,   A1 A2 B3
            """)
    void lasButacasSeparadasNoSeReservan(String caso, String codigos) {
        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Reserva(FUNCION, CLIENTE, butacas(codigos.split(" ")), CREADA));

        assertEquals("Las butacas tienen que ser contiguas y de la misma fila", error.getMessage());
    }

    // El contador tiene que llegar a cero cuando de verdad deja de poder pagarse, no siempre a los 15 minutos.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            recien hecha,                2026-08-14T10:00:00, 2026-08-14T10:00:00, 900
            a los cinco minutos,         2026-08-14T10:00:00, 2026-08-14T10:05:00, 600
            la funcion empieza antes,    2026-08-20T19:50:00, 2026-08-20T19:50:00, 600
            """)
    void quedaParaPagarLoQuePrimeroLlegue(String caso, LocalDateTime creada, LocalDateTime ahora, long segundos) {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, butacas("A1"), creada);

        assertEquals(segundos, reserva.segundosParaPagar(ahora));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            vencida,            2026-08-14T10:00:00, 2026-08-14T10:16:00
            funcion empezada,   2026-08-20T19:50:00, 2026-08-20T20:01:00
            """)
    void sinPoderPagarNoQuedaTiempo(String caso, LocalDateTime creada, LocalDateTime ahora) {
        assertNull(new Reserva(FUNCION, CLIENTE, butacas("A1"), creada).segundosParaPagar(ahora));
    }

    @Test
    void nacePorPagarConUnCodigoDeAccesoYSusButacasOcupadas() {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, butacas("A1", "A2"), CREADA);

        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
        assertEquals(8, reserva.getCodigo().length());
        assertEquals(Dinero.de(10000), reserva.getTotal());
        assertTrue(reserva.estaVigente());
    }

    // Creador: la reserva arma sus entradas con el precio que le da su función, butaca por tarifa.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            estándar general,  ESTANDAR, GENERAL,  5000
            vip general,       VIP,      GENERAL,  7500
            vip jubilado,      VIP,      JUBILADO, 3750
            """)
    void creaSusEntradasConElPrecioDeSuFuncion(String caso, TipoAsiento tipo, TipoTarifa tarifa, int precio) {
        Asiento butaca = new Asiento(SALA, 1, 1, tipo);

        Entrada entrada = new Reserva(FUNCION, CLIENTE, List.of(new ButacaPedida(butaca, tarifa)), CREADA)
                .getEntradas().get(0);

        assertEquals(tarifa, entrada.tarifa());
        assertEquals(Dinero.de(precio), entrada.precio());
    }

    // R13/R17: la vencida ya soltó sus butacas aunque todavía figure RESERVADA: no hay qué cancelar.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            recién hecha,        2026-08-14T10:00, true
            al filo de los 15,   2026-08-14T10:15, true
            vencida,             2026-08-14T10:16, false
            """)
    void seCancelaSoloMientrasNoVencio(String caso, LocalDateTime ahora, boolean cancelable) {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, butacas("A1"), CREADA);

        assertEquals(cancelable, reserva.esCancelable(ahora), caso);
    }

    @Test
    void laPagadaNoSeCancela() {
        assertFalse(pagada().esCancelable(CREADA));
    }

    // R17 sin scheduler: en la base sigue RESERVADA, pero hacia afuera la vencida ya venció.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            recién hecha,        2026-08-14T10:00, RESERVADA
            al filo de los 15,   2026-08-14T10:15, RESERVADA
            vencida,             2026-08-14T10:16, EXPIRADA
            """)
    void figuraVencidaApenasPasanLosQuinceMinutos(String caso, LocalDateTime ahora, EstadoReserva figura) {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, butacas("A1"), CREADA);

        assertEquals(figura, reserva.estadoA(ahora), caso);
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado(), caso);
        assertEquals(CREADA.plusMinutes(Reserva.MINUTOS_PARA_PAGAR), reserva.getVenceEn(), caso);
    }

    @Test
    void laPagadaYaNoVence() {
        assertEquals(EstadoReserva.PAGADA, pagada().estadoA(CREADA.plusDays(1)));
        assertNull(pagada().getVenceEn());
    }

    // La función es el 20/08 a las 20: el acomodador deja pasar ese día y no otro.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            a primera hora,          2026-08-20T00:00
            antes de empezar,        2026-08-20T19:45
            con la función empezada, 2026-08-20T20:15
            un minuto antes del fin, 2026-08-20T22:15
            """)
    void elDiaDeLaFuncionSeEntra(String caso, LocalDateTime cuando) {
        Reserva reserva = pagada();

        reserva.registrarIngreso(cuando);

        assertEquals(cuando, reserva.getIngresadaEn());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            dos días antes, 2026-08-18T20:00
            la víspera,     2026-08-19T23:59
            """)
    void otroDiaNoSeEntraNiSeGastaLaEntrada(String caso, LocalDateTime cuando) {
        Reserva reserva = pagada();

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> reserva.registrarIngreso(cuando));

        assertEquals("La función es el 20/08: se entra solo ese día", error.getMessage());
        assertNull(reserva.getIngresadaEn());
    }

    // R32: Matrix dura 136 minutos, así que la función del 20/08 a las 20 termina a las 22:16.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            paga antes del inicio,     PAGA,      2026-08-20T19:00, PAGADA
            paga durante la función,   PAGA,      2026-08-20T22:15, PAGADA
            paga justo al fin,         PAGA,      2026-08-20T22:16, NO_USADA
            paga días después,         PAGA,      2026-08-23T14:00, NO_USADA
            usada después del fin,     USADA,     2026-08-23T14:00, PAGADA
            sin pagar después del fin, SIN_PAGAR, 2026-08-23T14:00, EXPIRADA
            """)
    void laEntradaPagaQueNadieUsoVenceAlTerminarLaFuncion(String caso, String situacion, LocalDateTime ahora,
                                                         EstadoReserva esperado) {
        Reserva reserva = situacion.equals("SIN_PAGAR") ? new Reserva(FUNCION, CLIENTE, butacas("A1"), CREADA)
                : pagada();
        if (situacion.equals("USADA")) {
            reserva.registrarIngreso(LocalDateTime.of(2026, 8, 20, 19, 50));
        }

        assertEquals(esperado, reserva.estadoA(ahora));
    }

    // R32: la de ayer ya terminó, y lo dice antes que el día; la usada sigue diciendo cuándo se usó.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            justo al fin,   2026-08-20T22:16
            a la noche,     2026-08-20T23:30
            el día después, 2026-08-21T00:00
            """)
    void terminadaLaFuncionNoSeEntraNiSeGastaLaEntrada(String caso, LocalDateTime cuando) {
        Reserva reserva = pagada();

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> reserva.registrarIngreso(cuando));

        assertEquals("La función ya terminó: la entrada no sirve más", error.getMessage());
        assertNull(reserva.getIngresadaEn());
    }

    @Test
    void laUsadaDiceCuandoSeUsoAunqueLaFuncionHayaTerminado() {
        Reserva reserva = pagada();
        reserva.registrarIngreso(LocalDateTime.of(2026, 8, 20, 19, 50));

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> reserva.registrarIngreso(LocalDateTime.of(2026, 8, 21, 10, 0)));

        assertEquals("Esa entrada ya se usó el 20/08 19:50", error.getMessage());
    }

    private static Reserva pagada() {
        Reserva reserva = new Reserva(FUNCION, CLIENTE, butacas("A1"), CREADA);
        reserva.pagar();
        return reserva;
    }

    private static List<ButacaPedida> butacas(String... codigos) {
        List<ButacaPedida> butacas = new ArrayList<>();
        for (String codigo : codigos) {
            int fila = codigo.charAt(0) - 'A' + 1;
            int numero = Integer.parseInt(codigo.substring(1));
            butacas.add(new ButacaPedida(new Asiento(SALA, fila, numero, TipoAsiento.ESTANDAR), TipoTarifa.GENERAL));
        }
        return butacas;
    }

    private static List<ButacaPedida> primeras(int cuantas) {
        List<ButacaPedida> butacas = new ArrayList<>();
        for (int numero = 1; numero <= cuantas; numero++) {
            butacas.addAll(butacas("A" + numero));
        }
        return butacas;
    }
}
