package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.IndicadoresGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PropuestaGrilla;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Font;

// Lo que devolvió el planificador, a la derecha de los criterios: indicadores, elenco, géneros y la semana.
final class ResultadoGrilla extends JScrollPane {

    private final JPanel contenido = new AlAnchoDelVisor(null, false);

    ResultadoGrilla() {
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        setViewportView(contenido);
        setBorder(null);
        getVerticalScrollBar().setUnitIncrement(16);
    }

    /** Sin propuesta: los criterios cambiaron o el servidor la rechazó. */
    void vaciar() {
        contenido.removeAll();
        refrescar();
    }

    void esperar(boolean aplicando) {
        contenido.removeAll();
        agregar(aviso(aplicando ? "Creando las funciones…" : "Armando la grilla…", aplicando
                ? "Cada pase de la propuesta se programa como una función de verdad."
                : "Primero elige el elenco por puntaje y géneros; después llena cada sala día por día, preguntando "
                + "en cada horario si está libre."));
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        agregar(barra);
        refrescar();
    }

    void mostrar(PropuestaGrilla grilla, IndicadoresGrilla anterior) {
        contenido.removeAll();
        if (grilla.pases().isEmpty()) {
            agregar(aviso("Con estos criterios no entra ninguna función",
                    "Revisá la ventana horaria o los días."));
            refrescar();
            return;
        }
        boolean aplicada = grilla.funcionesCreadas() > 0;
        agregar(aviso(aplicada ? "Se crearon " + grilla.funcionesCreadas() + " funciones"
                        : "Así quedaría la semana: " + grilla.pases().size() + " funciones",
                aplicada ? "Las funciones ya están cargadas y se pueden ver en Funciones y en la Agenda."
                        : "Todavía no se escribió nada. Cambiá los criterios y volvé a previsualizar para comparar."));
        agregar(new TarjetasIndicadores(grilla.indicadores(), grilla.pases().size(), anterior));
        agregar(new ElencoSemana(grilla.elenco()));
        agregar(new PasesPorGenero(grilla.indicadores().pasesPorGenero()));
        agregar(new SemanaPorSala(grilla.pases()));
        refrescar();
    }

    private static JComponent aviso(String titulo, String texto) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel arriba = new JLabel(titulo);
        arriba.setFont(arriba.getFont().deriveFont(Font.BOLD, 15f));
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(Componentes.nota(texto), BorderLayout.CENTER);
        return Componentes.conBorde(panel);
    }

    private void agregar(JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        if (contenido.getComponentCount() > 0) contenido.add(Box.createVerticalStrut(12));
        contenido.add(componente);
    }

    private void refrescar() {
        contenido.revalidate();
        contenido.repaint();
    }
}
