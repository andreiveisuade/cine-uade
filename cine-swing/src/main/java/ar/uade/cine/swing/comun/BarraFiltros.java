package ar.uade.cine.swing.comun;

import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// La barra de filtros de un listado: cada filtro con su clave de la API, un Limpiar y el conteo al final.
/**
 * Cada filtro sabe leerse y vaciarse, así "Limpiar" no dispara una búsqueda por cada campo que vuelve a cero, y el
 * texto libre busca al dejar de tipear y no en cada tecla. Los valores salen con la clave que espera la API, en el
 * orden en que se agregaron.
 */
public final class BarraFiltros extends JPanel {

    private final JLabel conteo = new JLabel(" ");
    private final Map<String, Supplier<String>> valores = new LinkedHashMap<>();
    private final List<Runnable> vaciadores = new ArrayList<>();
    private final List<Timer> esperas = new ArrayList<>();
    private Runnable alCambiar = () -> { };
    private boolean callada;

    public BarraFiltros() {
        super(new FlujoConSalto());
        add(conteo);
    }

    /** Texto libre: busca cuando se deja de tipear, porque cada tecla sería un pedido. */
    public BarraFiltros texto(String etiqueta, String clave, JTextField campo) {
        agregar(new JLabel(etiqueta), campo);
        valores.put(clave, campo::getText);
        vaciadores.add(() -> campo.setText(""));
        esperas.add(Campos.alDejarDeTipear(campo, this::cambio));
        return this;
    }

    /** Un combo cuyo primer ítem es el "Todas" al que vuelve Limpiar; un combo es una decisión y busca enseguida. */
    public <T> BarraFiltros combo(String etiqueta, String clave, JComboBox<Opcion<T>> combo) {
        agregar(new JLabel(etiqueta), combo);
        valores.put(clave, () -> {
            T elegido = Campos.elegido(combo);
            return elegido == null ? null : elegido.toString();
        });
        vaciadores.add(() -> {
            if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
        });
        combo.addActionListener(e -> cambio());
        return this;
    }

    public BarraFiltros fecha(String etiqueta, String clave, JDateChooser selector) {
        agregar(new JLabel(etiqueta), selector);
        valores.put(clave, () -> Fechas.iso(selector));
        vaciadores.add(() -> selector.setDate(null));
        selector.addPropertyChangeListener("date", e -> cambio());
        return this;
    }

    public BarraFiltros boton(JButton boton) {
        agregar(boton);
        return this;
    }

    /** El botón que vuelve todo a cero sin buscar en el medio, y después corre {@code despues} una sola vez. */
    public BarraFiltros limpiar(Runnable despues) {
        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> {
            enSilencio(() -> vaciadores.forEach(Runnable::run));
            esperas.forEach(Timer::stop);
            despues.run();
        });
        return boton(limpiar);
    }

    public void alCambiar(Runnable accion) {
        this.alCambiar = accion;
    }

    /** Para llenar los combos desde el código sin que cada ítem agregado dispare una búsqueda. */
    public void enSilencio(Runnable cambios) {
        callada = true;
        try {
            cambios.run();
        } finally {
            callada = false;
        }
    }

    /** Clave de la API → lo elegido; lo vacío va null o "", y no viaja. */
    public Map<String, String> valores() {
        Map<String, String> leidos = new LinkedHashMap<>();
        valores.forEach((clave, valor) -> leidos.put(clave, valor.get()));
        return leidos;
    }

    public void mostrarConteo(String texto) {
        conteo.setText(texto);
    }

    // Todo antes del conteo, que queda siempre al final.
    private void agregar(JComponent... componentes) {
        for (JComponent componente : componentes) add(componente, getComponentCount() - 1);
    }

    private void cambio() {
        if (!callada) alCambiar.run();
    }
}
