package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// Sin Spring ni base: el algoritmo se prueba con una agenda vacía y el reloj fijo.
class RepartoDePasesTest {

    private static final LocalDate DIA = LocalDate.of(2026, 9, 1);

    // Una película de 100 minutos en una sala con 20 de limpieza, de 14 a 23: un pase cada dos horas
    // mientras el siguiente termine antes del cierre.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            el día entero desde la apertura,          2026-08-31T10:00, 14:00 16:00 18:00 20:00
            hoy a media tarde desde el próximo turno, 2026-09-01T15:10, 15:30 17:30 19:30
            hoy pasado el cierre no reparte nada,     2026-09-01T23:40, ''
            """)
    void repartePasesSeguidosConLimpiezaHastaElCierre(String caso, LocalDateTime ahora, String horarios) {
        Pelicula pelicula = new Pelicula("Una", 100, List.of(Genero.DRAMA), Clasificacion.ATP);
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 20);
        CriteriosGrilla criterios = new CriteriosGrilla(DIA, 1, LocalTime.of(14, 0), LocalTime.of(23, 0), 1,
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        List<PaseSugerido> pases = new RepartoDePases(List.of(pelicula), new PuntajeConfiable(List.of(pelicula)))
                .repartir(List.of(sala), Map.of(sala.getId(), AgendaDeSala.vacia()),
                        new VentanaDelDia(criterios, ahora));

        List<String> esperados = horarios.isBlank() ? List.of() : List.of(horarios.split(" "));
        assertEquals(esperados, pases.stream().map(p -> p.inicio().toLocalTime().toString()).toList(), caso);
    }
}
