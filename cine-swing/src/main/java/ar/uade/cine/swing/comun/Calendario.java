package ar.uade.cine.swing.comun;

import com.toedter.calendar.JCalendar;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;

// Un calendario del mes siempre a la vista, que las pantallas leen y ponen como LocalDate.
// Patrón Adapter: como CampoFecha, pero el Adaptee es el JCalendar desplegado (sin campo de texto).
public final class Calendario extends JPanel {

    private final JCalendar calendario;

    public Calendario(LocalDate inicial) {
        super(new BorderLayout());
        calendario = Fechas.calendario(inicial);
        add(calendario);
    }

    public LocalDate valor() {
        return Fechas.leer(calendario);
    }

    public void poner(LocalDate fecha) {
        Fechas.poner(calendario, fecha);
    }

    public void alCambiar(Runnable accion) {
        calendario.addPropertyChangeListener("calendar", e -> accion.run());
    }
}
