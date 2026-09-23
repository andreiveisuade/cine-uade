package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Funcion;
import ar.uade.cine.swing.api.dto.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Opcion;
import com.toedter.calendar.JCalendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import ar.uade.cine.swing.comun.SelectorDias;
import static ar.uade.cine.swing.comun.Formato.escapar;
import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;

/**
 * La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura, así se ve si dos funciones se
 * pisan (R3) y dónde entra algo nuevo. Dos modos: una sala toda la semana, o todas las salas un día; en una sola
 * columna, las funciones simultáneas de varias salas se pisarían.
 */
final class PantallaAgenda extends Pantalla {

    private static final double PX_POR_MINUTO = 1.1;
    // Rompe la proporción a propósito: en un corto de 5 minutos no se leería ni el título.
    private static final int ALTO_MINIMO = 26;
    private static final int ANCHO_HORAS = 56;
    private static final int ALTO_CABECERA = 44;
    private static final int MARGEN = 10;

    private record Columna(String titulo, String detalle, Predicate<Funcion> toma,
                           Function<Funcion, String> subtitulo) {
    }

    private final Navegacion navegacion;
    private final JComboBox<Opcion<Boolean>> modo = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JLabel etiquetaSala = new JLabel("Sala");
    private final JLabel conteo = new JLabel(" ");
    private final JCalendar calendario = Fechas.calendario(LocalDate.now());
    private final Grilla grilla = new Grilla();
    private final JLabel vacio = new JLabel("No hay funciones programadas en este rango.", JLabel.CENTER);
    private final JPanel lienzo = new JPanel(new BorderLayout());
    private LocalDate desde = LocalDate.now();
    private List<Sala> salas = List.of();
    // Cada pedido lleva el número de la vista que lo pidió: una respuesta lenta de una semana anterior no pisa la actual.
    private int vista;
    // Mover el calendario desde el código dispara su propio evento: sin esta marca, se redibujaría dos veces.
    private boolean sincronizando = true;

    PantallaAgenda(ApiHttp api, Navegacion navegacion) {
        super(api, "Agenda", "La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura. "
                + "Los huecos son dónde entra algo nuevo. Clic en un bloque para ver su borderó e informe.");
        this.navegacion = navegacion;

        modo.addItem(new Opcion<>(true, "Semana (una sala)"));
        modo.addItem(new Opcion<>(false, "Día (todas las salas)"));
        JButton anterior = new JButton("←");
        JButton hoy = new JButton("Hoy");
        JButton siguiente = new JButton("→");
        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Ver"));
        barra.add(modo);
        barra.add(etiquetaSala);
        barra.add(sala);
        barra.add(anterior);
        barra.add(hoy);
        barra.add(siguiente);
        barra.add(conteo);

        modo.addActionListener(e -> pintar());
        sala.addActionListener(e -> pintar());
        anterior.addActionListener(e -> mover(-diasDelModo()));
        siguiente.addActionListener(e -> mover(diasDelModo()));
        hoy.addActionListener(e -> ir(LocalDate.now()));
        calendario.addPropertyChangeListener("calendar", e -> {
            if (sincronizando) return;
            LocalDate elegido = Fechas.leer(calendario);
            if (elegido != null && !elegido.equals(desde)) {
                desde = elegido;
                pintar();
            }
        });

        vacio.setForeground(Componentes.gris());
        JScrollPane scroll = new JScrollPane(lienzo);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel izquierda = new JPanel(new BorderLayout(0, 8));
        izquierda.add(calendario, BorderLayout.NORTH);
        JLabel nota = new JLabel("<html><div style='width:210px'>El alto de cada bloque es su duración. Los que "
                + "duran menos de " + Math.round(ALTO_MINIMO / PX_POR_MINUTO) + " minutos se dibujan con un alto "
                + "mínimo para que el título entre: es la única parte del gráfico que no está a escala. Lo rayado es "
                + "la limpieza de la sala, que también ocupa.</div></html>");
        nota.setForeground(Componentes.gris());
        nota.setFont(nota.getFont().deriveFont(12f));
        nota.setVerticalAlignment(JLabel.TOP);
        izquierda.add(nota, BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(290, 0));

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(barra, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        add(izquierda, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);

        cargar(api::obtenerSalas, lista -> {
            salas = lista;
            sala.removeAllItems();
            salas.forEach(s -> sala.addItem(new Opcion<>(s.id(), s.nombre() + " — " + etiqueta(s.tipo()))));
            sincronizando = false;
            pintar();
        });
    }

    private boolean porSemana() {
        Boolean elegido = Campos.elegido(modo);
        return elegido == null || elegido;
    }

    private int diasDelModo() {
        return porSemana() ? 7 : 1;
    }

    private void mover(int dias) {
        ir(desde.plusDays(dias));
    }

    private void ir(LocalDate fecha) {
        desde = fecha;
        sincronizando = true;
        Fechas.poner(calendario, fecha);
        sincronizando = false;
        pintar();
    }

    // Pide solo el rango que se ve, con los filtros de la API: una semana de una sala, o un día de todas.
    private void pintar() {
        if (sincronizando) return;
        boolean semana = porSemana();
        etiquetaSala.setVisible(semana);
        sala.setVisible(semana);
        Integer salaId = Campos.elegido(sala);
        Sala elegida = salas.stream().filter(s -> salaId != null && s.id() == salaId).findFirst()
                .orElse(salas.isEmpty() ? null : salas.get(0));

        LocalDate primerDia = desde;
        List<Columna> columnas = new ArrayList<>();
        if (!semana) {
            for (Sala s : salas) {
                columnas.add(new Columna(s.nombre(), etiqueta(s.tipo()),
                        f -> f.sala().id() == s.id() && dia(f).equals(primerDia), f -> etiqueta(f.proyeccion())));
            }
        } else if (elegida != null) {
            for (int i = 0; i < 7; i++) {
                LocalDate fecha = primerDia.plusDays(i);
                columnas.add(new Columna(SelectorDias.abreviatura(fecha.getDayOfWeek()),
                        String.valueOf(fecha.getDayOfMonth()),
                        f -> dia(f).equals(fecha),
                        f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma()).toLowerCase()));
            }
        }
        int pedida = ++vista;
        if (columnas.isEmpty()) {
            dibujar(columnas, List.of(), "");
            return;
        }

        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("desde", primerDia.toString());
        filtros.put("hasta", (semana ? primerDia.plusDays(6) : primerDia).toString());
        if (semana) filtros.put("salaId", String.valueOf(elegida.id()));
        String donde = semana ? " en " + elegida.nombre() : "";
        cargar(() -> api.obtenerFunciones(filtros), funciones -> {
            if (pedida == vista) dibujar(columnas, funciones, donde);
        });
    }

    private void dibujar(List<Columna> columnas, List<Funcion> funciones, String donde) {
        List<Funcion> visibles = funciones.stream().filter(f -> columnas.stream().anyMatch(c -> c.toma().test(f)))
                .toList();
        conteo.setText(visibles.size() + " funciones" + donde);

        lienzo.removeAll();
        if (visibles.isEmpty()) {
            lienzo.add(vacio);
        } else {
            grilla.mostrar(columnas, visibles);
            lienzo.add(grilla);
        }
        lienzo.revalidate();
        lienzo.repaint();
    }

    private static LocalDate dia(Funcion f) {
        return LocalDateTime.parse(f.inicio()).toLocalDate();
    }

    private static int minutosDe(String iso) {
        LocalTime hora = LocalDateTime.parse(iso).toLocalTime();
        return hora.getHour() * 60 + hora.getMinute();
    }

    private static String enHora(int minutos) {
        return String.format("%02d:%02d", (minutos / 60) % 24, minutos % 60);
    }

    /** La grilla dibujada a mano: los bloques son componentes (para el clic y el tooltip), el resto se pinta. */
    private final class Grilla extends JPanel {

        private record Bloque(int columna, Funcion funcion, JLabel etiqueta) {
        }

        private List<Columna> columnas = List.of();
        private final List<Bloque> bloques = new ArrayList<>();
        private int inicio;
        private int fin;

        Grilla() {
            super(null);
        }

        void mostrar(List<Columna> nuevas, List<Funcion> visibles) {
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

        private JLabel bloque(Funcion f, Columna columna) {
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
                    navegacion.abrir(new PantallaFuncion(api, navegacion, f.id()));
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
    }
}
