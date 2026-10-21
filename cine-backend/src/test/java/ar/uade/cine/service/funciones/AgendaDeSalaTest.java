package ar.uade.cine.service.funciones;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AgendaDeSalaTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            una función de dos horas,       2026-09-09T21:00, 2026-09-09T23:00
            una que cruza la medianoche,    2026-09-09T23:30, 2026-09-10T01:30
            un rango de varios días,        2026-09-01T00:00, 2026-09-30T23:59
            """)
    void laAgendaVaciaNuncaChoca(String caso, LocalDateTime inicio, LocalDateTime fin) {
        assertFalse(AgendaDeSala.vacia().chocaEn(inicio, fin), caso);
    }
}
