package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Solo formato y obligatoriedad: que un precio negativo o cero pase acá es a propósito, porque esa regla es del
 * backend. Sin Swing: cada lectura es texto que entra y valor o motivo que sale.
 */
class LecturasTest {

    @Test
    void unObligatorioVacioOEnBlancoFalta() {
        assertEquals("Falta completar «Título».", Lecturas.leerTexto("   ", "Título", true).error());
        assertTrue(Lecturas.leerTexto("", "Dirección", false).valida());
        assertNull(Lecturas.leerTexto("", "Dirección", false).valor());
        assertEquals("Matrix", Lecturas.leerTexto("  Matrix ", "Título", true).valor());
    }

    @Test
    void unNumeroMalTipeadoNoViajaComoVacio() {
        Lectura<Double> precio = Lecturas.leerDecimal("abc", "Precio", true);

        assertFalse(precio.valida());
        assertEquals("«Precio»: «abc» no es un número.", precio.error());
    }

    @Test
    void elDecimalAceptaComaYPunto() {
        assertEquals(2500.5, Lecturas.leerDecimal("2500,5", "Precio", true).valor());
        assertEquals(2500.5, Lecturas.leerDecimal("2500.5", "Precio", true).valor());
        assertFalse(Lecturas.leerDecimal("25,00,1", "Precio", true).valida());
    }

    @Test
    void lasReglasDeNegocioNoSeAnticipan() {
        // Cero o negativo es un número bien escrito: si vale lo dice el gestor del backend.
        assertEquals(0.0, Lecturas.leerDecimal("0", "Precio", true).valor());
        assertEquals(-5, Lecturas.leerEntero("-5", "Minutos de limpieza", false).valor());
    }

    @Test
    void unEnteroConDecimalesOEnormeSeRechaza() {
        assertFalse(Lecturas.leerEntero("7.5", "Días", false).valida());
        assertFalse(Lecturas.leerEntero("99999999999", "Días", false).valida());
        assertNull(Lecturas.leerEntero(" ", "Días", false).valor());
    }

    @Test
    void enUnaListaUnElementoInvalidoRechazaTodoYSeNombra() {
        Lectura<List<Integer>> filas = Lecturas.leerEnteros("8,x,12", "Butacas por fila", true);

        assertFalse(filas.valida());
        assertEquals("«Butacas por fila»: «x» no es un número entero.", filas.error());
    }

    @Test
    void unaListaBienEscritaSeLeeEntera() {
        assertEquals(List.of(8, 10, 12), Lecturas.leerEnteros(" 8, 10 ,12", "Butacas por fila", true).valor());
        assertEquals("«Butacas por fila»: hay un valor vacío entre comas.",
                Lecturas.leerEnteros("8,,12", "Butacas por fila", true).error());
        assertEquals("Falta completar «Butacas por fila».",
                Lecturas.leerEnteros("", "Butacas por fila", true).error());
    }

    @Test
    void losCodigosDeButacaSeNormalizanYSeRechazaLoQueNoLoEs() {
        assertEquals(List.of("A1", "B12"), Lecturas.leerCodigos("a1, B12", "Butacas VIP").valor());
        assertEquals(List.of(), Lecturas.leerCodigos("", "Butacas VIP").valor());
        assertEquals("«Butacas VIP»: «1A» no es un código de butaca (fila y número, como A1).",
                Lecturas.leerCodigos("A1,1A", "Butacas VIP").error());
    }

    @Test
    void laHoraVaEnHHmm() {
        assertEquals("09:05", Lecturas.leerHora("9:05", "Desde hora", false).valor());
        assertEquals("21:30", Lecturas.leerHora("21:30", "Desde hora", false).valor());
        assertFalse(Lecturas.leerHora("25:00", "Desde hora", false).valida());
        assertNull(Lecturas.leerHora("", "Desde hora", false).valor());
    }

    @Test
    void elEmailTieneQueParecerUnEmail() {
        assertTrue(Lecturas.leerEmail("ana@mail.com", "Cliente", false).valida());
        assertFalse(Lecturas.leerEmail("ana", "Cliente", false).valida());
        assertTrue(Lecturas.leerEmail("", "Cliente", false).valida());
    }
}
