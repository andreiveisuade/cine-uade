package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.PaseSugerido;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
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

// Los pases de la propuesta por día y, adentro, por sala, en el orden en que los manda el backend.
final class SemanaPorSala extends JPanel {

    SemanaPorSala(List<PaseSugerido> pases) {
        super(new BorderLayout(0, 4));
        // JEditorPane y no JLabel: el HTML de un JLabel no corta línea, y una sala con ocho pases no entra a lo ancho.
        JEditorPane texto = new JEditorPane();
        // Sin esto, cargar el texto deja el cursor al final y el scroll salta al último día.
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        texto.setContentType("text/html");
        texto.setText(html(pases));
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        texto.setFont(getFont());
        add(Componentes.subtitulo("La semana, sala por sala"), BorderLayout.NORTH);
        add(texto, BorderLayout.CENTER);
    }

    private static String html(List<PaseSugerido> pases) {
        Map<LocalDate, Map<String, List<PaseSugerido>>> porDia = new LinkedHashMap<>();
        for (PaseSugerido p : pases) {
            porDia.computeIfAbsent(LocalDateTime.parse(p.inicio()).toLocalDate(), d -> new LinkedHashMap<>())
                    .computeIfAbsent(p.sala(), s -> new ArrayList<>()).add(p);
        }
        String gris = Colores.hex(Colores.secundario());
        StringBuilder html = new StringBuilder("<html>");
        porDia.forEach((fecha, salas) -> {
            int cuantos = salas.values().stream().mapToInt(List::size).sum();
            html.append("<p style='margin-top:8px'><b>").append(dia(fecha)).append("</b> · ")
                    .append(cuantos).append(" pases</p><table>");
            salas.forEach((sala, deLaSala) -> html.append("<tr><td valign='top' nowrap><font color='")
                    .append(gris).append("'>").append(escapar(sala)).append("</font></td><td>")
                    .append(deLaSala.stream()
                            .map(p -> "<b>" + hora(p.inicio()) + "</b> " + escapar(p.titulo()))
                            .collect(Collectors.joining(" &nbsp;·&nbsp; "))).append("</td></tr>"));
            html.append("</table>");
        });
        return html.append("</html>").toString();
    }
}
