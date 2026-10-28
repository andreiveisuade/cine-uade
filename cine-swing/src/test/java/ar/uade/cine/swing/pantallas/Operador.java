package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.Mensajes;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Maneja una pantalla como lo haría el encargado, sin ventana: busca campos por su etiqueta y botones por su texto.
 * Todo corre en el EDT; los pedidos van en SwingWorker, así que lo que muestran se espera con {@link #esperar}.
 */
public final class Operador {

    private final JComponent pantalla;
    private final JLabel barra;
    private final List<Throwable> errores = new ArrayList<>();
    private final Thread.UncaughtExceptionHandler anterior = Thread.getDefaultUncaughtExceptionHandler();

    private Operador(Supplier<? extends JComponent> armar) {
        // Lo que tira un callback en el EDT (un diálogo en headless, un null) no llega al test: se junta acá.
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            synchronized (errores) {
                errores.add(error);
            }
        });
        AtomicReference<JComponent> armada = new AtomicReference<>();
        AtomicReference<JLabel> barraDeEstado = new AtomicReference<>();
        enEdt(() -> {
            // Una raíz con su barra de estado, como la de VentanaPrincipal: ahí avisa Mensajes.exito.
            JRootPane raiz = new JRootPane();
            JPanel contenido = new JPanel(new BorderLayout());
            barraDeEstado.set(Mensajes.barraDeEstado(raiz));
            armada.set(armar.get());
            contenido.add(armada.get(), BorderLayout.CENTER);
            raiz.setContentPane(contenido);
        });
        pantalla = armada.get();
        barra = barraDeEstado.get();
    }

    public static Operador en(Supplier<? extends JComponent> armar) {
        return new Operador(armar);
    }

    public JComponent pantalla() {
        return pantalla;
    }

    /** Para el @AfterEach: nada reventó en el EDT. */
    public void cerrar() {
        enEdt(() -> { });
        Thread.setDefaultUncaughtExceptionHandler(anterior);
        synchronized (errores) {
            assertEquals(List.of(), errores);
        }
    }

    // --- acciones ---

    public Operador tipear(String etiqueta, String texto) {
        enEdt(() -> ((JTextComponent) campo(etiqueta)).setText(texto));
        return this;
    }

    public Operador elegir(String etiqueta, String opcion) {
        enEdt(() -> {
            JComboBox<?> combo = (JComboBox<?>) campo(etiqueta);
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (String.valueOf(combo.getItemAt(i)).equals(opcion)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
            throw new AssertionError("El combo " + etiqueta + " no tiene " + opcion);
        });
        return this;
    }

    public Operador fecha(String etiqueta, LocalDate fecha) {
        enEdt(() -> ((CampoFecha) campo(etiqueta)).poner(fecha));
        return this;
    }

    public Operador hora(String etiqueta, LocalTime hora) {
        enEdt(() -> ((JSpinner) campo(etiqueta)).setValue(
                Date.from(LocalDate.now().atTime(hora).atZone(ZoneId.systemDefault()).toInstant())));
        return this;
    }

    public Operador tildar(String texto) {
        enEdt(() -> boton(texto).doClick());
        return this;
    }

    public Operador clic(String texto) {
        enEdt(() -> boton(texto).doClick());
        return this;
    }

    public Operador elegirFila(int fila) {
        enEdt(() -> tabla().setRowSelectionInterval(fila, fila));
        return this;
    }

    // --- lo que se ve ---

    /** Espera a que se cumpla, mirando desde el EDT: los pedidos vuelven en SwingWorker. */
    public void esperar(BooleanSupplier condicion) {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            AtomicReference<Boolean> cumplida = new AtomicReference<>(false);
            enEdt(() -> cumplida.set(condicion.getAsBoolean()));
            if (cumplida.get()) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("No pasó en 5 s. Se ve: " + textos());
    }

    /** Espera a que algún texto de la pantalla (etiqueta, botón o barra) contenga {@code parte}. */
    public void esperarTexto(String parte) {
        esperar(() -> textos().stream().anyMatch(t -> t.contains(parte)));
    }

    public void esperarFilas(int cantidad) {
        esperar(() -> tabla().getRowCount() == cantidad);
    }

    public String barra() {
        return barra.getText();
    }

    public List<String> textos() {
        return Stream.concat(Stream.of(barra.getText()), componentes(pantalla)
                        .filter(Operador::visible)
                        .flatMap(c -> c instanceof JLabel l ? Stream.of(l.getText())
                                : c instanceof AbstractButton b ? Stream.of(b.getText()) : Stream.empty()))
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    /** Los textos largos de solo lectura: un informe, un detalle, un ticket. */
    public List<String> areas() {
        List<String> areas = new ArrayList<>();
        enEdt(() -> componentes(pantalla).filter(c -> c instanceof JTextArea).filter(Operador::visible)
                .forEach(c -> areas.add(((JTextArea) c).getText())));
        return areas;
    }

    public void esperarArea(String parte) {
        esperar(() -> componentes(pantalla).filter(c -> c instanceof JTextArea).filter(Operador::visible)
                .anyMatch(c -> ((JTextArea) c).getText().contains(parte)));
    }

    public String valor(String etiqueta) {
        AtomicReference<String> leido = new AtomicReference<>();
        enEdt(() -> {
            Component c = campo(etiqueta);
            leido.set(c instanceof JComboBox<?> combo ? String.valueOf(combo.getSelectedItem())
                    : c instanceof CampoFecha f ? f.iso() : ((JTextComponent) c).getText());
        });
        return leido.get();
    }

    public List<String> opciones(String etiqueta) {
        List<String> opciones = new ArrayList<>();
        enEdt(() -> {
            JComboBox<?> combo = (JComboBox<?>) campo(etiqueta);
            for (int i = 0; i < combo.getItemCount(); i++) opciones.add(String.valueOf(combo.getItemAt(i)));
        });
        return opciones;
    }

    /** La celda como se ve: la fila y la columna por su título. */
    public String celda(int fila, String columna) {
        AtomicReference<String> leida = new AtomicReference<>();
        enEdt(() -> {
            JTable t = tabla();
            leida.set(String.valueOf(t.getValueAt(fila, t.getColumnModel().getColumnIndex(columna))));
        });
        return leida.get();
    }

    public int filas() {
        AtomicReference<Integer> filas = new AtomicReference<>();
        enEdt(() -> filas.set(tabla().getRowCount()));
        return filas.get();
    }

    /** Un botón por su texto o un campo por su etiqueta. */
    public boolean habilitado(String texto) {
        AtomicReference<Boolean> habilitado = new AtomicReference<>();
        enEdt(() -> habilitado.set(buscar(AbstractButton.class, b -> texto.equals(b.getText()))
                .map(Component::isEnabled).orElseGet(() -> campo(texto).isEnabled())));
        return habilitado.get();
    }

    public boolean hayBoton(String texto) {
        AtomicReference<Boolean> hay = new AtomicReference<>();
        enEdt(() -> hay.set(buscar(AbstractButton.class, b -> texto.equals(b.getText())).isPresent()));
        return hay.get();
    }

    public <C extends Component> C buscar(Class<C> tipo) {
        return buscar(tipo, c -> true).orElseThrow(() -> new AssertionError("No hay " + tipo.getSimpleName()));
    }

    // --- búsqueda en el árbol ---

    private JTable tabla() {
        return buscar(JTable.class);
    }

    private AbstractButton boton(String texto) {
        return buscar(AbstractButton.class, b -> texto.equals(b.getText()))
                .orElseThrow(() -> new AssertionError("No hay botón " + texto + ". Se ve: " + textos()));
    }

    /** El primer campo que sigue a la etiqueta, en el orden en que está armada la pantalla; el asterisco no cuenta. */
    private Component campo(String etiqueta) {
        List<Component> todos = componentes(pantalla).filter(Operador::visible).toList();
        for (int i = 0; i < todos.size(); i++) {
            // "Sala *" pide la obligatoria del formulario, no el filtro "Sala" de la barra.
            if (todos.get(i) instanceof JLabel l && l.getText() != null
                    && (l.getText().equals(etiqueta) || l.getText().replace(" *", "").equals(etiqueta))) {
                return todos.subList(i + 1, todos.size()).stream()
                        .filter(c -> c instanceof JTextComponent || c instanceof JComboBox<?>
                                || c instanceof CampoFecha || c instanceof JSpinner)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("La etiqueta " + etiqueta + " no tiene campo"));
            }
        }
        throw new AssertionError("No hay campo " + etiqueta + ". Se ve: " + textos());
    }

    private <C extends Component> Optional<C> buscar(Class<C> tipo, Predicate<C> condicion) {
        return componentes(pantalla).filter(tipo::isInstance).map(tipo::cast)
                .filter(Operador::visible).filter(condicion).findFirst();
    }

    private static Stream<Component> componentes(Component raiz) {
        return Stream.concat(Stream.of(raiz), raiz instanceof Container c
                ? Stream.of(c.getComponents()).flatMap(Operador::componentes) : Stream.empty());
    }

    // Visible con todos sus padres: la tarjeta de otro tipo de promoción o un botón escondido no cuentan.
    private static boolean visible(Component c) {
        for (Component actual = c; actual != null; actual = actual.getParent()) {
            if (!actual.isVisible()) return false;
        }
        return true;
    }

    private static void enEdt(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(accion);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            if (e.getCause() instanceof Error r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
