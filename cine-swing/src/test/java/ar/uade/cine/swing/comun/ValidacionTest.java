package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.comun.Validacion.Lectura;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Solo formato y obligatoriedad: que un precio negativo o cero pase acá es a propósito, porque esa regla es del
 * backend. Los campos se crean sin ventana, así corre headless.
 */
class ValidacionTest {

    @Test
    void unObligatorioVacioOEnBlancoFalta() {
        assertEquals("Falta completar «Título».", Validacion.leerTexto("   ", "Título", true).error());
        assertTrue(Validacion.leerTexto("", "Dirección", false).valida());
        assertNull(Validacion.leerTexto("", "Dirección", false).valor());
        assertEquals("Matrix", Validacion.leerTexto("  Matrix ", "Título", true).valor());
    }

    @Test
    void unNumeroMalTipeadoNoViajaComoVacio() {
        Lectura<Double> precio = Validacion.leerDecimal("abc", "Precio", true);

        assertFalse(precio.valida());
        assertEquals("«Precio»: «abc» no es un número.", precio.error());
    }

    @Test
    void elDecimalAceptaComaYPunto() {
        assertEquals(2500.5, Validacion.leerDecimal("2500,5", "Precio", true).valor());
        assertEquals(2500.5, Validacion.leerDecimal("2500.5", "Precio", true).valor());
        assertFalse(Validacion.leerDecimal("25,00,1", "Precio", true).valida());
    }

    @Test
    void lasReglasDeNegocioNoSeAnticipan() {
        // Cero o negativo es un número bien escrito: si vale lo dice el gestor del backend.
        assertEquals(0.0, Validacion.leerDecimal("0", "Precio", true).valor());
        assertEquals(-5, Validacion.leerEntero("-5", "Minutos de limpieza", false).valor());
    }

    @Test
    void unEnteroConDecimalesOEnormeSeRechaza() {
        assertFalse(Validacion.leerEntero("7.5", "Días", false).valida());
        assertFalse(Validacion.leerEntero("99999999999", "Días", false).valida());
        assertNull(Validacion.leerEntero(" ", "Días", false).valor());
    }

    @Test
    void enUnaListaUnElementoInvalidoRechazaTodoYSeNombra() {
        Lectura<List<Integer>> filas = Validacion.leerEnteros("8,x,12", "Butacas por fila", true);

        assertFalse(filas.valida());
        assertEquals("«Butacas por fila»: «x» no es un número entero.", filas.error());
    }

    @Test
    void unaListaBienEscritaSeLeeEntera() {
        assertEquals(List.of(8, 10, 12), Validacion.leerEnteros(" 8, 10 ,12", "Butacas por fila", true).valor());
        assertEquals("«Butacas por fila»: hay un valor vacío entre comas.",
                Validacion.leerEnteros("8,,12", "Butacas por fila", true).error());
        assertEquals("Falta completar «Butacas por fila».",
                Validacion.leerEnteros("", "Butacas por fila", true).error());
    }

    @Test
    void losCodigosDeButacaSeNormalizanYSeRechazaLoQueNoLoEs() {
        assertEquals(List.of("A1", "B12"), Validacion.leerCodigos("a1, B12", "Butacas VIP").valor());
        assertEquals(List.of(), Validacion.leerCodigos("", "Butacas VIP").valor());
        assertEquals("«Butacas VIP»: «1A» no es un código de butaca (fila y número, como A1).",
                Validacion.leerCodigos("A1,1A", "Butacas VIP").error());
    }

    @Test
    void laHoraVaEnHHmm() {
        assertEquals("09:05", Validacion.leerHora("9:05", "Desde hora", false).valor());
        assertEquals("21:30", Validacion.leerHora("21:30", "Desde hora", false).valor());
        assertFalse(Validacion.leerHora("25:00", "Desde hora", false).valida());
        assertNull(Validacion.leerHora("", "Desde hora", false).valor());
    }

    @Test
    void elEmailTieneQueParecerUnEmail() {
        assertTrue(Validacion.leerEmail("ana@mail.com", "Cliente", false).valida());
        assertFalse(Validacion.leerEmail("ana", "Cliente", false).valida());
        assertTrue(Validacion.leerEmail("", "Cliente", false).valida());
    }

    @Test
    void conErroresNoSeMandaYSeMarcanLosCampos() {
        JLabel mensaje = new JLabel(" ");
        JTextField titulo = new JTextField("");
        JTextField precio = new JTextField("abc");
        JTextField director = new JTextField("");
        Validacion v = new Validacion(mensaje);

        v.texto(titulo, "Título", true);
        v.decimal(precio, "Precio", true);
        v.texto(director, "Dirección", false);

        assertFalse(v.ok());
        assertTrue(Validacion.marcado(titulo));
        assertTrue(Validacion.marcado(precio));
        assertFalse(Validacion.marcado(director));
        assertEquals(List.of("Falta completar «Título».", "«Precio»: «abc» no es un número."), v.errores());
        assertTrue(mensaje.getText().contains("Falta completar «Título»."));
    }

    @Test
    void corregirElCampoLeSacaLaMarca() {
        JTextField titulo = new JTextField("");
        Validacion v = new Validacion(new JLabel());
        v.texto(titulo, "Título", true);
        v.ok();

        titulo.setText("Matrix");

        assertFalse(Validacion.marcado(titulo));
    }

    @Test
    void sinErroresSeMandaYElMensajeQuedaVacio() {
        JLabel mensaje = new JLabel("algo de antes");
        Validacion v = new Validacion(mensaje);

        Integer duracion = v.entero(new JTextField("136"), "Duración", true);

        assertTrue(v.ok());
        assertEquals(136, duracion);
        assertEquals(" ", mensaje.getText());
    }

    @Test
    void unComboVacioYUnGrupoSinTildarCuentanComoFaltantes() {
        JComboBox<Opcion<Integer>> sala = new JComboBox<>();
        JPanel generos = new JPanel();
        generos.add(new JCheckBox("Acción"));
        Validacion v = new Validacion(new JLabel());

        assertNull(v.elegido(sala, "Sala"));
        v.exigir(false, generos, "Géneros");

        assertFalse(v.ok());
        assertTrue(Validacion.marcado(sala));
        assertTrue(Validacion.marcado(generos));
    }

    @Test
    void elErrorDelBackendSeMuestraTalCualYMarcaElCampoQueNombra() {
        JLabel mensaje = new JLabel();
        JTextField nombre = new JTextField("Pochoclos");
        JTextField precio = new JTextField("0");
        Validacion v = new Validacion(mensaje);
        v.texto(nombre, "Nombre", true);
        v.decimal(precio, "Precio base", true);
        assertTrue(v.ok());

        v.mostrarError(new ErrorApi(400, "El precio debe ser mayor a cero"));

        assertTrue(mensaje.getText().contains("El precio debe ser mayor a cero"));
        assertTrue(Validacion.marcado(precio));
        assertFalse(Validacion.marcado(nombre));
    }

    @Test
    void unErrorDelBackendQueNoNombraNingunCampoNoMarcaNada() {
        JTextField nombre = new JTextField("Sala 1");
        Validacion v = new Validacion(new JLabel());
        v.texto(nombre, "Nombre", true);
        v.ok();

        v.mostrarError(new ErrorApi(409, "Ya hay una sala llamada Sala 1"));

        assertFalse(Validacion.marcado(nombre));
    }

    @Test
    void losFiltrosNoDejanTipearLoQueNoEsNumero() {
        JTextField entero = Campos.soloEntero(new JTextField());
        JTextField decimal = Campos.soloDecimal(new JTextField());
        JTextField lista = Campos.soloListaDeEnteros(new JTextField());

        entero.setText("12a");
        decimal.setText("2500,5");
        lista.setText("8, 10,12");

        assertEquals("", entero.getText());
        assertEquals("2500,5", decimal.getText());
        assertEquals("8, 10,12", lista.getText());

        lista.setText("8,x");
        assertEquals("8, 10,12", lista.getText());
    }
}
