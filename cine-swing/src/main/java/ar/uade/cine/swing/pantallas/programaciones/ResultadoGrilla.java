package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.IndicadoresGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PropuestaGrilla;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

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
        agregar(aviso(aplicando ? "Creando las funciones…" : "Armando la semana…", null));
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        agregar(barra);
        refrescar();
    }

    /** {@code pedidas} es null si el campo quedó vacío y el backend usó su valor por defecto. */
    void mostrar(PropuestaGrilla grilla, IndicadoresGrilla anterior, Integer pedidas) {
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
                aplicada ? "Ya están en Funciones y en la Agenda." : "Todavía no se creó nada."));
        // Con menos confirmadas que las pedidas el elenco sale corto: sin esto parece un error del planificador.
        if (pedidas != null && grilla.elenco().size() < pedidas) {
            agregar(alerta("Pediste " + pedidas + " películas y hay " + grilla.elenco().size()
                    + " confirmadas: confirmá más en Por revisar."));
        }
        agregar(new TarjetasIndicadores(grilla.indicadores(), anterior));
        agregar(new ElencoSemana(grilla.elenco(), grilla.pases().size()));
        agregar(new PasesPorGenero(grilla.indicadores().pasesPorGenero(), grilla.pases().size()));
        agregar(new SemanaPorSala(grilla.pases()));
        refrescar();
    }

    private static JComponent aviso(String titulo, String texto) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(Componentes.subtitulo(titulo), BorderLayout.NORTH);
        if (texto != null) panel.add(Componentes.nota(texto), BorderLayout.CENTER);
        return Componentes.conBorde(panel);
    }

    private static JComponent alerta(String texto) {
        JLabel etiqueta = Componentes.texto(texto);
        etiqueta.setForeground(Colores.aviso());
        return etiqueta;
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
