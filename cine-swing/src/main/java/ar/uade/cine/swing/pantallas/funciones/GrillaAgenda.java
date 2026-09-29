package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.pantallas.Navegacion;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;
import static ar.uade.cine.swing.comun.Formato.escapar;

// La grilla de la Agenda, dibujada a mano: cada bloque de función mide en alto lo que dura.
/** Los bloques son componentes para tener clic y tooltip; las horas, las columnas y la limpieza se pintan. */
final class GrillaAgenda extends JPanel {

    static final double PX_POR_MINUTO = 1.1;
    // Rompe la proporción a propósito: en un corto de 5 minutos no se leería ni el título.
    static final int ALTO_MINIMO = 26;
    private static final int ANCHO_HORAS = 56;
    private static final int ALTO_CABECERA = 44;
    private static final int MARGEN = 10;

    private record Bloque(int columna, Funcion funcion, JLabel etiqueta) {
    }

    private final Navegacion navegacion;
    private List<ColumnaAgenda> columnas = List.of();
    private final List<Bloque> bloques = new ArrayList<>();
    private int inicio;
    private int fin;

    GrillaAgenda(Navegacion navegacion) {
        super(null);
        this.navegacion = navegacion;
    }

    void mostrar(List<ColumnaAgenda> nuevas, List<Funcion> visibles) {
        columnas = nuevas;
        removeAll();
        bloques.clear();
        // Sale de las funciones y no de 00 a 24: un cine abre a la tarde. Incluye la limpieza de la última.
        inicio = visibles.stream().mapToInt(f -> minutosDe(f.inicio())).min().orElse(0) / 60 * 60;
        int ultimo = visibles.stream().mapToInt(f -> minutosDe(f.inicio()) + f.pelicula().duracionMinutos()
                + f.sala().minutosLimpieza()).max().orElse(0);
        fin = (int) Math.ceil(ultimo / 60.0) * 60;
        for (Funcion f : visibles) {
            for (int i = 0; i < columnas.size(); i++) {
                if (columnas.get(i).toma().test(f)) bloques.add(new Bloque(i, f, bloque(f, columnas.get(i))));
            }
        }
        bloques.forEach(b -> add(b.etiqueta()));
        revalidate();
        repaint();
    }

    private JLabel bloque(Funcion f, ColumnaAgenda columna) {
        int arranca = minutosDe(f.inicio());
        int dura = f.pelicula().duracionMinutos();
        boolean alto = dura * PX_POR_MINUTO > 44;
        JLabel etiqueta = new JLabel("<html><b>" + enHora(arranca) + "</b> " + escapar(f.pelicula().titulo())
                + (alto ? "<br><font size='-2'>" + columna.subtitulo().apply(f) + "</font>" : "") + "</html>");
        etiqueta.setVerticalAlignment(JLabel.TOP);
        etiqueta.setOpaque(true);
        // Un color estable por película entre recargas: el id decide cuál.
        etiqueta.setBackground(Colores.fondoPelicula(f.pelicula().id()));
        etiqueta.setForeground(Colores.textoPelicula());
        etiqueta.setFont(etiqueta.getFont().deriveFont(12f));
        etiqueta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Colores.bordePelicula(f.pelicula().id())),
                BorderFactory.createEmptyBorder(2, 6, 2, 4)));
        etiqueta.setToolTipText("<html>" + escapar(f.pelicula().titulo()) + "<br>" + enHora(arranca) + "–"
                + enHora(arranca + dura) + " (" + duracion(dura) + ")<br>" + escapar(f.sala().nombre()) + " · "
                + etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma()) + " — clic para ver su borderó</html>");
        etiqueta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        etiqueta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navegacion.abrirInforme(f.id());
            }
        });
        return etiqueta;
    }

    private int anchoColumna() {
        return Math.max((getWidth() - ANCHO_HORAS) / Math.max(columnas.size(), 1), 60);
    }

    private int y(int minutos) {
        return ALTO_CABECERA + MARGEN + (int) Math.round((minutos - inicio) * PX_POR_MINUTO);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Math.max(720, ANCHO_HORAS + 60 * columnas.size()),
                y(fin) + MARGEN);
    }

    @Override
    public void doLayout() {
        int ancho = anchoColumna();
        for (Bloque b : bloques) {
            int arranca = minutosDe(b.funcion().inicio());
            int alto = (int) Math.max(b.funcion().pelicula().duracionMinutos() * PX_POR_MINUTO, ALTO_MINIMO);
            b.etiqueta().setBounds(ANCHO_HORAS + b.columna() * ancho + 2, y(arranca), ancho - 4, alto);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int ancho = anchoColumna();
        Color borde = Colores.borde();
        Color gris = Componentes.gris();
        Font base = getFont();

        g2.setColor(new Color(gris.getRed(), gris.getGreen(), gris.getBlue(), 30));
        g2.fillRect(0, 0, getWidth(), ALTO_CABECERA);
        for (int i = 0; i < columnas.size(); i++) {
            int x = ANCHO_HORAS + i * ancho;
            g2.setColor(borde);
            g2.drawLine(x, 0, x, getHeight());
            g2.setColor(getForeground());
            g2.setFont(base.deriveFont(Font.BOLD, 13f));
            centrar(g2, columnas.get(i).titulo(), x, ancho, 18);
            g2.setFont(base.deriveFont(12f));
            g2.setColor(gris);
            centrar(g2, columnas.get(i).detalle(), x, ancho, 36);
        }
        g2.setColor(borde);
        g2.drawLine(0, ALTO_CABECERA, getWidth(), ALTO_CABECERA);

        g2.setFont(base.deriveFont(11f));
        for (int m = inicio; m <= fin; m += 60) {
            int y = y(m);
            g2.setColor(new Color(borde.getRed(), borde.getGreen(), borde.getBlue(), 128));
            g2.drawLine(ANCHO_HORAS, y, getWidth(), y);
            g2.setColor(gris);
            String texto = enHora(m);
            g2.drawString(texto, ANCHO_HORAS - 6 - g2.getFontMetrics().stringWidth(texto), y + 4);
        }

        // Sin la limpieza rayada el hueco parece libre, y la regla aparece recién cuando el alta falla.
        for (Bloque b : bloques) {
            int limpieza = b.funcion().sala().minutosLimpieza();
            if (limpieza <= 0) continue;
            int termina = minutosDe(b.funcion().inicio()) + b.funcion().pelicula().duracionMinutos();
            int x = ANCHO_HORAS + b.columna() * ancho + 2;
            int arriba = y(termina);
            int alto = (int) Math.round(limpieza * PX_POR_MINUTO);
            Graphics2D rayado = (Graphics2D) g2.create(x, arriba, ancho - 4, alto);
            rayado.setColor(Colores.limpieza());
            rayado.setStroke(new BasicStroke(2));
            for (int d = -alto; d < ancho; d += 8) rayado.drawLine(d, alto, d + alto, 0);
            rayado.setColor(gris);
            rayado.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                    new float[]{3, 3}, 0));
            rayado.drawLine(0, 0, ancho, 0);
            rayado.dispose();
        }
        g2.dispose();
    }

    private void centrar(Graphics2D g2, String texto, int x, int ancho, int y) {
        int w = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, x + (ancho - w) / 2, y);
    }

    private static int minutosDe(String iso) {
        LocalTime hora = LocalDateTime.parse(iso).toLocalTime();
        return hora.getHour() * 60 + hora.getMinute();
    }

    private static String enHora(int minutos) {
        return String.format("%02d:%02d", (minutos / 60) % 24, minutos % 60);
    }
}
