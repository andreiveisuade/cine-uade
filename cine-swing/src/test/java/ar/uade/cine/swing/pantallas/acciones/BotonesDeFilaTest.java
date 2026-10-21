package ar.uade.cine.swing.pantallas.acciones;

import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El Invoker: sin fila no hace nada, lo preguntado no se manda si el encargado dice que no, y lo que sale bien recarga.
class BotonesDeFilaTest {

    // Una acción de mentira que alterna: anota qué filas ejecutó.
    private static final class Alternar implements AccionDeFila<String> {

        private final List<String> ejecutadas = new ArrayList<>();
        private final boolean pregunta;

        Alternar(boolean pregunta) {
            this.pregunta = pregunta;
        }

        @Override
        public String texto() {
            return "Alternar";
        }

        @Override
        public String texto(String fila) {
            return "Alternar " + fila;
        }

        @Override
        public boolean habilitada(String fila) {
            return !fila.equals("bloqueada");
        }

        @Override
        public Optional<Pregunta> pregunta(String fila) {
            return pregunta ? Optional.of(new Pregunta("¿Seguro?", "Sí")) : Optional.empty();
        }

        @Override
        public void ejecutar(String fila) {
            synchronized (ejecutadas) {
                ejecutadas.add(fila);
            }
        }

        @Override
        public String hecho(String fila) {
            return "Hecho";
        }
    }

    private final Tabla<String> tabla = new Tabla<>(Columna.<String>de("Fila", s -> s));
    private final List<Pregunta> preguntadas = new ArrayList<>();
    private final CountDownLatch recargo = new CountDownLatch(1);

    private JButton armar(Alternar accion, boolean contesta) {
        tabla.mostrar(List.of("una", "bloqueada"));
        JPanel panel = new JPanel();
        new BotonesDeFila<>(panel, tabla, recargo::countDown, p -> {
            preguntadas.add(p);
            return contesta;
        }, List.of(accion)).agregarA(panel);
        return (JButton) panel.getComponent(0);
    }

    private void elegir(int fila) {
        tabla.tabla().setRowSelectionInterval(fila, fila);
    }

    @Test
    void sinFilaElegidaNoHaceNada() throws Exception {
        Alternar accion = new Alternar(true);
        JButton boton = armar(accion, true);

        SwingUtilities.invokeAndWait(boton::doClick);

        assertTrue(preguntadas.isEmpty());
        assertFalse(recargo.await(300, TimeUnit.MILLISECONDS));
        assertTrue(accion.ejecutadas.isEmpty());
    }

    @Test
    void conLaPreguntaRechazadaNoEjecuta() throws Exception {
        Alternar accion = new Alternar(true);
        JButton boton = armar(accion, false);
        elegir(0);

        SwingUtilities.invokeAndWait(boton::doClick);

        assertEquals("¿Seguro?", preguntadas.get(0).texto());
        assertEquals("Cancelar", preguntadas.get(0).no());
        assertFalse(recargo.await(300, TimeUnit.MILLISECONDS));
        assertTrue(accion.ejecutadas.isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "con la pregunta aceptada,   true",
            "sin pregunta no pregunta,   false"})
    void ejecutaLaFilaElegidaYRecarga(String caso, boolean pregunta) throws Exception {
        Alternar accion = new Alternar(pregunta);
        JButton boton = armar(accion, true);
        elegir(0);

        SwingUtilities.invokeAndWait(boton::doClick);

        assertTrue(recargo.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("una"), accion.ejecutadas);
        assertEquals(pregunta ? 1 : 0, preguntadas.size());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "sin fila apagado con el texto de siempre,  -1, false, Alternar",
            "con fila dice cuál va a hacer,              0, true,  Alternar una",
            "la acción decide si se puede,               1, false, Alternar bloqueada"})
    void refrescarSigueALaFilaElegida(String caso, int fila, boolean habilitado, String texto) throws Exception {
        tabla.mostrar(List.of("una", "bloqueada"));
        JPanel panel = new JPanel();
        BotonesDeFila<String> botones = new BotonesDeFila<>(panel, tabla, recargo::countDown, p -> true,
                List.of(new Alternar(false)));
        botones.agregarA(panel);
        if (fila >= 0) elegir(fila);

        botones.refrescar();

        JButton boton = (JButton) panel.getComponent(0);
        assertEquals(habilitado, boton.isEnabled());
        assertEquals(texto, boton.getText());
    }
}
