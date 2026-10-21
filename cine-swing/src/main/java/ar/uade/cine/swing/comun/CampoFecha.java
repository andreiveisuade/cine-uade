package ar.uade.cine.swing.comun;

import com.toedter.calendar.JDateChooser;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;

// Un campo para elegir un día, que las pantallas leen y ponen como LocalDate.
// Patrón Adapter: traduce LocalDate (pantallas) a java.util.Date de JDateChooser, con Fechas.
public final class CampoFecha extends JPanel {

    private final JDateChooser selector;

    /** {@code inicial} null deja el campo vacío, que en un filtro significa "sin filtro". */
    public CampoFecha(LocalDate inicial) {
        super(new BorderLayout());
        selector = Fechas.selector(inicial);
        add(selector);
    }

    public LocalDate valor() {
        return Fechas.leer(selector);
    }

    public void poner(LocalDate fecha) {
        Fechas.poner(selector, fecha);
    }

    /** Lo que viaja al backend: ISO, o null si el campo quedó vacío. */
    public String iso() {
        return Fechas.iso(selector);
    }

    public void alCambiar(Runnable accion) {
        selector.addPropertyChangeListener("date", e -> accion.run());
    }

    // Lo que se tipea: es lo que Validacion marca en rojo y enfoca.
    JComponent editor() {
        return (JComponent) selector.getDateEditor().getUiComponent();
    }
}
