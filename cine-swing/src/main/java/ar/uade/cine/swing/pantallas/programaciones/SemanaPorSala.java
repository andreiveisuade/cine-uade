package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.PaseSugerido;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.escapar;
import static ar.uade.cine.swing.comun.Formato.hora;

// Las funciones de la propuesta, una pestaña por día y adentro por sala, en el orden en que las manda el backend.
// Pestañas y no todo seguido: siete días de seis salas eran un scroll de pantallas.
final class SemanaPorSala extends JPanel {

    SemanaPorSala(List<PaseSugerido> pases) {
        super(new BorderLayout(0, 6));
        Map<LocalDate, Map<String, List<PaseSugerido>>> porDia = new LinkedHashMap<>();
        for (PaseSugerido p : pases) {
            porDia.computeIfAbsent(LocalDateTime.parse(p.inicio()).toLocalDate(), d -> new LinkedHashMap<>())
                    .computeIfAbsent(p.sala(), s -> new ArrayList<>()).add(p);
        }
        JTabbedPane dias = new JTabbedPane();
        porDia.forEach((fecha, salas) -> dias.addTab(dia(fecha) + " · "
                + salas.values().stream().mapToInt(List::size).sum(), texto(salas)));
        add(Componentes.subtitulo("Funciones por día"), BorderLayout.NORTH);
        add(dias, BorderLayout.CENTER);
    }

    // JEditorPane y no JLabel: el HTML de un JLabel no corta línea, y una sala con ocho funciones no entra a lo ancho.
    private JEditorPane texto(Map<String, List<PaseSugerido>> salas) {
        JEditorPane texto = new JEditorPane();
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        texto.setContentType("text/html");
        texto.setText(html(salas));
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        texto.setFont(getFont());
        return texto;
    }

    private static String html(Map<String, List<PaseSugerido>> salas) {
        String gris = Colores.hex(Colores.secundario());
        StringBuilder html = new StringBuilder("<html><table cellpadding='3'>");
        salas.forEach((sala, deLaSala) -> html.append("<tr><td valign='top' nowrap><font color='")
                .append(gris).append("'>").append(escapar(sala)).append("</font></td><td>")
                .append(deLaSala.stream()
                        .map(p -> "<b>" + hora(p.inicio()) + "</b> " + escapar(p.titulo()))
                        .collect(Collectors.joining(" &nbsp;·&nbsp; "))).append("</td></tr>"));
        return html.append("</table></html>").toString();
    }
}
