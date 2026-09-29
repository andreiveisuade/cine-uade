package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;
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
 * Qué se marca, qué se muestra y adónde va cada rechazo. Los campos se crean sin ventana, así corre headless.
 */
class ValidacionTest {

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
        assertTrue(Marcas.marcado(titulo));
        assertTrue(Marcas.marcado(precio));
        assertFalse(Marcas.marcado(director));
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

        assertFalse(Marcas.marcado(titulo));
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
        assertTrue(Marcas.marcado(sala));
        assertTrue(Marcas.marcado(generos));
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

        v.mostrarError(new ErrorApi(400, "El precio tiene que ser mayor a cero"));

        assertTrue(mensaje.getText().contains("El precio tiene que ser mayor a cero"));
        assertTrue(Marcas.marcado(precio));
        assertFalse(Marcas.marcado(nombre));
    }

    @Test
    void unErrorDelBackendQueNoNombraNingunCampoNoMarcaNada() {
        JTextField nombre = new JTextField("Sala 1");
        Validacion v = new Validacion(new JLabel());
        v.texto(nombre, "Nombre", true);
        v.ok();

        v.mostrarError(new ErrorApi(400, "Ya hay una sala con esa distribución de butacas"));

        assertFalse(Marcas.marcado(nombre));
    }

    @Test
    void unNombreRepetidoEsDelFormularioAunqueSea409() {
        JLabel mensaje = new JLabel();
        JTextField titulo = new JTextField("Matrix");
        JTextField duracion = new JTextField("136");
        Validacion v = new Validacion(mensaje);
        v.texto(titulo, "Título *", true);
        v.entero(duracion, "Duración", true);
        v.ok();
        ErrorApi repetido = new ErrorApi(409, "Ya existe una película con ese título");

        assertTrue(v.esDelFormulario(repetido));
        v.mostrarError(repetido);

        assertTrue(mensaje.getText().contains("Ya existe una película con ese título"));
        assertTrue(Marcas.marcado(titulo));
        assertFalse(Marcas.marcado(duracion));
    }

    // Lo que no es del formulario va al diálogo, que headless no se puede abrir: se prueba la decisión.
    @Test
    void un409QueNoNombraNingunCampoEsGlobal() {
        JComboBox<Opcion<String>> medio = new JComboBox<>();
        medio.addItem(new Opcion<>("EFECTIVO", "Efectivo"));
        Validacion v = new Validacion(new JLabel());
        v.elegido(medio, "Medio de pago");
        v.ok();

        assertFalse(v.esDelFormulario(
                new ErrorApi(409, "La reserva cambió mientras se procesaba: volvé a intentarlo")));
        assertFalse(v.esDelFormulario(new ErrorApi(500, "Falló el servidor")));
        assertTrue(v.esDelFormulario(new ErrorApi(400, "No se puede cobrar una reserva cancelada")));
    }
}
