package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// Sin Spring ni base: el algoritmo se prueba con una agenda vacía y el reloj fijo.
class RepartoDePasesTest {

    private static final LocalDate DIA = LocalDate.of(2026, 9, 1);

    // Película de 100 min, sala con 20 de limpieza, de 14 a 23: un pase cada dos horas mientras termine antes del cierre.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            el día entero desde la apertura,          2026-08-31T10:00, 14:00 16:00 18:00 20:00
            hoy a media tarde desde el próximo turno, 2026-09-01T15:10, 15:30 17:30 19:30
            hoy pasado el cierre no reparte nada,     2026-09-01T23:40, ''
            """)
    void repartePasesSeguidosConLimpiezaHastaElCierre(String caso, LocalDateTime ahora, String horarios) {
        List<PaseSugerido> pases = repartir(new Pelicula("Una", 100, List.of(Genero.DRAMA), Clasificacion.ATP),
                new Sala("Sala 1", TipoSala.DOS_D, 20), ahora);

        List<String> esperados = horarios.isBlank() ? List.of() : List.of(horarios.split(" "));
        assertEquals(esperados, pases.stream().map(p -> p.inicio().toLocalTime().toString()).toList(), caso);
    }

    // R8 y R31: el formato de cada pase sale de la sala y de la película, no del pedido.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            con versión 3D en una sala 3D va en 3D,        TRES_D,   DOS_D;TRES_D, TRES_D
            con versión 3D en la IMAX va en 3D,             IMAX,     DOS_D;TRES_D, TRES_D
            con versión 3D en una sala 2D va en 2D,         DOS_D,    DOS_D;TRES_D, DOS_D
            sin versión 3D una sala 3D la da en 2D,         TRES_D,   DOS_D,        DOS_D
            solo en 3D no entra en una sala 2D,             DOS_D,    TRES_D,       ''
            R35: con versión 3D en una sala 4D va en 3D,    CUATRO_D, DOS_D;TRES_D, TRES_D
            R35: sin versión 3D no entra en una sala 4D,    CUATRO_D, DOS_D,        ''
            """)
    void elFormatoDeCadaPaseSaleDeLaSalaYDeLaPelicula(String caso, TipoSala tipo, String formatos, String esperado) {
        Pelicula pelicula = new Pelicula("Una", 100, List.of(Genero.DRAMA), Clasificacion.ATP);
        pelicula.cambiarFormatos(Arrays.stream(formatos.split(";")).map(Proyeccion::valueOf).toList());

        List<PaseSugerido> pases = repartir(pelicula, new Sala("Sala 1", tipo, 20),
                LocalDateTime.of(2026, 8, 31, 10, 0));

        assertEquals(esperado.isBlank() ? List.of() : List.of(Proyeccion.valueOf(esperado)),
                pases.stream().map(PaseSugerido::proyeccion).distinct().toList(), caso);
    }

    private static List<PaseSugerido> repartir(Pelicula pelicula, Sala sala, LocalDateTime ahora) {
        CriteriosGrilla criterios = new CriteriosGrilla(DIA, 1, LocalTime.of(14, 0), LocalTime.of(23, 0), 1,
                Version.SUBTITULADA, Dinero.de(5000));
        return new RepartoDePases(List.of(pelicula), new PuntajeConfiable(List.of(pelicula)))
                .repartir(List.of(sala), Map.of(sala.getId(), AgendaDeSala.vacia()),
                        new VentanaDelDia(criterios, ahora));
    }
}
