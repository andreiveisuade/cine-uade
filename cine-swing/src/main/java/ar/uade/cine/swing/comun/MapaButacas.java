package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.dto.salas.Asiento;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** La sala vista de frente, fila por fila, como MapaButacas.jsx. Quien la usa decide cómo se pinta cada butaca. */
public final class MapaButacas extends JPanel {

    // `punteada`: la butaca no se vende (fuera de servicio u ocupada), aunque siga siendo clicable.
    public record Estilo(Color fondo, Color borde, boolean habilitada, boolean punteada, String tooltip) {
    }

    public static final Map<String, String> SIMBOLO = Map.of("VIP", "*", "PAREJA", "&", "ACCESIBLE", "+",
            "ESTANDAR", "");

    public MapaButacas(int filas, List<Asiento> asientos, Function<Asiento, Estilo> pintar,
                       Consumer<Asiento> alElegir) {
        super(new GridBagLayout());
        GridBagConstraints pantalla = new GridBagConstraints();
        pantalla.gridy = 0;
        pantalla.fill = GridBagConstraints.HORIZONTAL;
        pantalla.insets = new Insets(0, 0, 16, 0);
        JLabel etiqueta = new JLabel("P A N T A L L A", SwingConstants.CENTER);
        etiqueta.setOpaque(true);
        etiqueta.setBackground(Colores.pantallaDeSala());
        etiqueta.setForeground(Colores.textoPantallaDeSala());
        etiqueta.setFont(etiqueta.getFont().deriveFont(10f));
        add(etiqueta, pantalla);

        for (int fila = 1; fila <= filas; fila++) {
            int numero = fila;
            List<Asiento> deLaFila = asientos.stream().filter(a -> a.fila() == numero).toList();
            JPanel renglon = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            JLabel letra = new JLabel(deLaFila.isEmpty() ? "" : deLaFila.get(0).codigo().substring(0, 1));
            letra.setPreferredSize(new Dimension(16, 28));
            renglon.add(letra);
            for (Asiento asiento : deLaFila) {
                Estilo estilo = pintar.apply(asiento);
                JButton boton = new JButton(asiento.numero() + SIMBOLO.getOrDefault(asiento.tipo(), ""));
                boton.setMargin(new Insets(0, 0, 0, 0));
                boton.setFont(boton.getFont().deriveFont(Font.PLAIN, 10f));
                boton.setPreferredSize(new Dimension("PAREJA".equals(asiento.tipo()) ? 48 : 30, 28));
                boton.setBackground(estilo.fondo());
                // Punteado lo que no se vende: se distingue por la forma y no solo por el color.
                boton.setBorder(estilo.habilitada() && !estilo.punteada()
                        ? BorderFactory.createLineBorder(estilo.borde())
                        : BorderFactory.createDashedBorder(estilo.borde(), 3, 2));
                if (estilo.punteada()) boton.setForeground(Colores.secundario());
                boton.setFocusPainted(false);
                boton.setEnabled(estilo.habilitada());
                boton.setToolTipText(estilo.tooltip());
                boton.addActionListener(e -> alElegir.accept(asiento));
                renglon.add(boton);
            }
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = fila;
            add(renglon, c);
        }
    }

    public static Color colorTipo(String tipo) {
        return Colores.butaca(tipo);
    }
}
