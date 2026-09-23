package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.IndicadoresGrilla;
import ar.uade.cine.swing.api.dto.PaseSugerido;
import ar.uade.cine.swing.api.dto.PedidoGrilla;
import ar.uade.cine.swing.api.dto.PeliculaElegida;
import ar.uade.cine.swing.api.dto.PropuestaGrilla;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tarea;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.duracion;
import static ar.uade.cine.swing.comun.Formato.hora;

/**
 * Planificador de la semana: el backend elige el elenco y reparte los pases; acá solo se piden y se muestran.
 * Determinista: lo que muestra Previsualizar es exactamente lo que crea Aplicar.
 */
final class PantallaPlanificador extends Pantalla {

    private record Idiomas(List<String> idiomas, List<String> proyecciones) {
    }

    // Estático y no de la pantalla: comparar corridas es el uso normal y sobrevive a salir y volver.
    private static IndicadoresGrilla corridaAnterior;

    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JTextField dias = new JTextField("7");
    private final JSpinner apertura = Fechas.hora(LocalTime.of(14, 0));
    private final JSpinner cierre = Fechas.hora(LocalTime.MIDNIGHT);
    private final JTextField cuantasPeliculas = new JTextField("8");
    private final JTextField precioBase = new JTextField("5000");
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JButton previsualizar = new JButton("Previsualizar");
    private final JButton aplicar = new JButton("Aplicar");
    private final JPanel resultado = new AlAnchoDelVisor();
    private PropuestaGrilla propuesta;
    // Cada cambio de criterio sube la versión: una respuesta de criterios viejos se descarta al llegar.
    private int version;

    PantallaPlanificador(ApiHttp api) {
        super(api, "Planificador de la semana", "Elige el elenco con un criterio que mira <b>puntaje y géneros a la "
                + "vez</b> y reparte los pases entre las salas de forma proporcional al puntaje: la mejor de la semana "
                + "se lleva cuatro o cinco funciones diarias y la última, una. No pisa funciones ya cargadas.");
        resultado.setLayout(new BoxLayout(resultado, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(resultado);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(criterios(), BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        cargar(() -> new Idiomas(api.obtenerIdiomas(), api.obtenerProyecciones()), c -> {
            Opcion.de(c.idiomas(), v -> etiqueta(v)).forEach(idioma::addItem);
            Opcion.de(c.proyecciones(), v -> etiqueta(v)).forEach(proyeccion::addItem);
            version++;
        });
    }

    private JPanel criterios() {
        previsualizar.addActionListener(e -> correr(false));
        aplicar.addActionListener(e -> correr(true));
        aplicar.setEnabled(false);
        JPanel botones = new JPanel(new GridLayout(2, 1, 0, 6));
        botones.add(previsualizar);
        botones.add(aplicar);

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(Componentes.subtitulo("Criterios"))
                .campo("Desde", desde)
                .campo("Días", dias)
                .campo("Apertura", apertura)
                .campo("Cierre", cierre)
                .ancho(Componentes.nota("El cierre a las 00:00 se lee como el final del día. Es hasta cuándo tiene "
                        + "que <i>haber terminado</i> la última función, no cuándo puede empezar."))
                .campo("Cuántas películas", cuantasPeliculas)
                .ancho(Componentes.nota("Títulos distintos en la semana. Solo se eligen entre las confirmadas."))
                .campo("Precio base", precioBase)
                .campo("Idioma", idioma)
                .campo("Proyección", proyeccion)
                .ancho(botones)
                .ancho(Componentes.nota("Previsualizar no escribe nada. Al aplicar, el servidor <b>vuelve a "
                        + "calcular</b> la propuesta con estos mismos criterios: si alguien programó algo en el "
                        + "medio, lo respeta."))
                .cerrar();

        Runnable cambio = this::criteriosCambiados;
        desde.addPropertyChangeListener("date", e -> cambio.run());
        apertura.addChangeListener(e -> cambio.run());
        cierre.addChangeListener(e -> cambio.run());
        idioma.addActionListener(e -> cambio.run());
        proyeccion.addActionListener(e -> cambio.run());
        DocumentListener alTipear = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cambio.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cambio.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cambio.run();
            }
        };
        dias.getDocument().addDocumentListener(alTipear);
        cuantasPeliculas.getDocument().addDocumentListener(alTipear);
        precioBase.getDocument().addDocumentListener(alTipear);

        JPanel panel = Componentes.conBorde(formulario);
        panel.setPreferredSize(new Dimension(340, 0));
        return panel;
    }

    private void criteriosCambiados() {
        version++;
        if (propuesta == null) return;
        propuesta = null;
        aplicar.setText("Aplicar");
        aplicar.setEnabled(false);
        resultado.removeAll();
        resultado.revalidate();
        resultado.repaint();
    }

    private PedidoGrilla pedido() {
        LocalTime abre = Fechas.leerHora(apertura);
        LocalTime cierra = Fechas.leerHora(cierre);
        return new PedidoGrilla(Fechas.iso(desde), Campos.entero(dias), abre == null ? null : abre.toString(),
                cierra == null ? null : cierra.toString(), Campos.entero(cuantasPeliculas),
                Campos.decimal(precioBase), Campos.elegido(idioma), Campos.elegido(proyeccion));
    }

    private void correr(boolean aplicando) {
        PedidoGrilla pedido = pedido();
        int pedidaEn = version;
        previsualizar.setEnabled(false);
        aplicar.setEnabled(false);
        mostrarEspera(aplicando);
        Tarea.ejecutar(this, () -> aplicando ? api.armarGrilla(pedido) : api.proponerGrilla(pedido), grilla -> {
            previsualizar.setEnabled(true);
            if (pedidaEn != version) return;
            IndicadoresGrilla anterior = corridaAnterior;
            corridaAnterior = grilla.indicadores();
            propuesta = grilla;
            // funcionesCreadas es 0 al previsualizar: es lo único que distingue «así quedaría» de «así quedó».
            boolean aplicada = grilla.funcionesCreadas() > 0;
            aplicar.setText(aplicada ? "Aplicada" : "Crear " + grilla.pases().size() + " funciones");
            aplicar.setEnabled(!aplicada && !grilla.pases().isEmpty());
            mostrar(grilla, anterior);
            if (aplicando) avisar("Se crearon " + grilla.funcionesCreadas() + " funciones");
        }, error -> {
            previsualizar.setEnabled(true);
            propuesta = null;
            aplicar.setText("Aplicar");
            resultado.removeAll();
            if (!error.esSesionVencida()) agregar(error("No se pudo", error.getMessage()));
            refrescar();
        });
    }

    private void mostrarEspera(boolean aplicando) {
        resultado.removeAll();
        agregar(aviso(aplicando ? "Creando las funciones…" : "Armando la grilla…", aplicando
                ? "Cada pase de la propuesta se programa como una función de verdad."
                : "Primero elige el elenco por puntaje y géneros; después llena cada sala día por día, preguntando "
                + "en cada horario si está libre."));
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        agregar(barra);
        refrescar();
    }

    private void mostrar(PropuestaGrilla grilla, IndicadoresGrilla anterior) {
        resultado.removeAll();
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
        agregar(indicadores(grilla.indicadores(), grilla.pases().size(), anterior));
        agregar(elenco(grilla.elenco()));
        agregar(generos(grilla.indicadores().pasesPorGenero()));
        agregar(pases(grilla.pases()));
        refrescar();
    }

    private JPanel indicadores(IndicadoresGrilla i, int pases, IndicadoresGrilla anterior) {
        JPanel tarjetas = new JPanel(new GridLayout(2, 2, 8, 8));
        tarjetas.add(tarjeta("Ocupación de las salas", porcentaje(i.ocupacion()),
                String.format(Locale.ROOT, "%,d de %,d minutos libres", i.minutosProgramados(),
                        i.minutosDisponibles()).replace(',', '.'),
                variacion(i, anterior, IndicadoresGrilla::ocupacion, PantallaPlanificador::porcentaje)));
        tarjetas.add(tarjeta("Puntaje promedio", conDecimal(i.puntajePromedio()),
                "por pase: una película con más funciones pesa más",
                variacion(i, anterior, IndicadoresGrilla::puntajePromedio, PantallaPlanificador::conDecimal)));
        tarjetas.add(tarjeta("Géneros cubiertos", i.generosCubiertos() + " de " + i.generosTotales(),
                "géneros del catálogo que aparecen en la semana",
                variacion(i, anterior, x -> (double) x.generosCubiertos(), d -> String.valueOf(Math.round(d)))));
        tarjetas.add(tarjeta("Pases", String.valueOf(pases), "funciones que arma la propuesta", null));
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjetas.getPreferredSize().height));
        return tarjetas;
    }

    private static String variacion(IndicadoresGrilla actual, IndicadoresGrilla anterior,
                                    Function<IndicadoresGrilla, Double> valor, Function<Double, String> formato) {
        if (anterior == null) return null;
        double delta = valor.apply(actual) - valor.apply(anterior);
        if (Math.abs(delta) < 0.0001) return "igual que la corrida anterior";
        return (delta > 0 ? "▲ " : "▼ ") + formato.apply(Math.abs(delta)) + " vs. la corrida anterior";
    }

    private static JPanel tarjeta(String titulo, String valor, String detalle, String variacion) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel arriba = new JLabel(titulo.toUpperCase());
        arriba.setForeground(Componentes.gris());
        arriba.setFont(arriba.getFont().deriveFont(11f));
        JLabel numero = new JLabel(valor);
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, 24f));
        panel.add(arriba);
        panel.add(numero);
        panel.add(new JLabel("<html><div style='width:150px'>" + detalle + "</div></html>"));
        if (variacion != null) {
            JLabel cambio = new JLabel(variacion);
            cambio.setFont(cambio.getFont().deriveFont(11f));
            panel.add(cambio);
        }
        return Componentes.conBorde(panel);
    }

    private JComponent elenco(List<PeliculaElegida> elenco) {
        Tabla<PeliculaElegida> tabla = new Tabla<>(
                Columna.<PeliculaElegida>de("Película", p -> p.titulo()).ancho(240),
                Columna.<PeliculaElegida>numero("Puntaje", p -> conDecimal(p.puntaje())),
                Columna.<PeliculaElegida>de("Duración", p -> duracion(p.duracionMinutos())),
                Columna.<PeliculaElegida>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                        .collect(Collectors.joining(" · "))).ancho(220),
                Columna.<PeliculaElegida>numero("Pases", PeliculaElegida::pases));
        tabla.mostrar(elenco);
        JScrollPane scroll = tabla.conScroll();
        int alto = 30 + 26 * elenco.size();
        scroll.setPreferredSize(new Dimension(600, alto));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(Componentes.subtitulo("Elenco de la semana (" + elenco.size() + ")"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(Componentes.nota("La primera entró por puntaje; las siguientes, por lo que <b>agregan</b> a lo ya "
                + "elegido: por eso puede entrar una comedia de 7,0 antes que la cuarta de acción de 8,5. Los pases se "
                + "reparten proporcionalmente al puntaje."), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent generos(Map<String, Integer> pasesPorGenero) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        if (pasesPorGenero == null || pasesPorGenero.isEmpty()) return panel;
        panel.add(Componentes.izquierda(Componentes.subtitulo("Pases por género")));
        List<Map.Entry<String, Integer>> entradas = new ArrayList<>(pasesPorGenero.entrySet());
        entradas.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        int maximo = entradas.get(0).getValue();
        for (Map.Entry<String, Integer> e : entradas) {
            JPanel fila = new JPanel(new BorderLayout(8, 0));
            JLabel nombre = new JLabel(etiqueta(e.getKey()));
            nombre.setPreferredSize(new Dimension(130, nombre.getPreferredSize().height));
            JProgressBar barra = new JProgressBar(0, maximo);
            barra.setValue(e.getValue());
            barra.setStringPainted(true);
            barra.setString(String.valueOf(e.getValue()));
            fila.add(nombre, BorderLayout.WEST);
            fila.add(barra, BorderLayout.CENTER);
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            fila.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            panel.add(Componentes.izquierda(fila));
        }
        panel.add(Componentes.izquierda(Componentes.nota("Una película cuenta en todos sus géneros, así que la suma "
                + "es mayor que la cantidad de pases.")));
        return panel;
    }

    // Por día y, adentro, por sala, en el orden en que llegan: el backend ya los manda ordenados.
    private JComponent pases(List<PaseSugerido> pases) {
        Map<String, Map<String, List<PaseSugerido>>> porDia = new LinkedHashMap<>();
        for (PaseSugerido p : pases) {
            porDia.computeIfAbsent(p.inicio().substring(0, 10), d -> new LinkedHashMap<>())
                    .computeIfAbsent(p.sala(), s -> new ArrayList<>()).add(p);
        }
        StringBuilder html = new StringBuilder("<html>");
        porDia.forEach((fecha, salas) -> {
            int cuantos = salas.values().stream().mapToInt(List::size).sum();
            html.append("<p style='margin-top:8px'><b>").append(dia(fecha + "T00:00:00")).append("</b> · ")
                    .append(cuantos).append(" pases</p><table>");
            salas.forEach((sala, deLaSala) -> html.append("<tr><td valign='top' nowrap><font color='gray'>").append(sala)
                    .append("</font></td><td>").append(deLaSala.stream()
                            .map(p -> "<b>" + hora(p.inicio()) + "</b> " + escapar(p.titulo()))
                            .collect(Collectors.joining(" &nbsp;·&nbsp; "))).append("</td></tr>"));
            html.append("</table>");
        });
        html.append("</html>");
        // JEditorPane y no JLabel: el HTML de un JLabel no corta línea, y una sala con ocho pases no entra a lo ancho.
        JEditorPane texto = new JEditorPane();
        // Sin esto, cargar el texto deja el cursor al final y el scroll salta al último día.
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        texto.setContentType("text/html");
        texto.setText(html.toString());
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        texto.setFont(getFont());
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(Componentes.subtitulo("La semana, sala por sala"), BorderLayout.NORTH);
        panel.add(texto, BorderLayout.CENTER);
        return panel;
    }

    private static JComponent aviso(String titulo, String texto) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel arriba = new JLabel(titulo);
        arriba.setFont(arriba.getFont().deriveFont(Font.BOLD, 15f));
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(Componentes.nota(texto), BorderLayout.CENTER);
        return Componentes.conBorde(panel);
    }

    private static JComponent error(String titulo, String texto) {
        JComponent panel = aviso(titulo, texto);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Colores.error()),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        return panel;
    }

    private void agregar(JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        if (resultado.getComponentCount() > 0) resultado.add(Box.createVerticalStrut(12));
        resultado.add(componente);
    }

    private void refrescar() {
        resultado.revalidate();
        resultado.repaint();
    }

    private static String porcentaje(double fraccion) {
        return Math.round(fraccion * 100) + "%";
    }

    private static String conDecimal(double numero) {
        return String.format(Locale.ROOT, "%.1f", numero).replace('.', ',');
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Sigue el ancho del scroll en vez de estirarlo: sin esto, lo más ancho del resultado empuja todo hacia afuera. */
    private static final class AlAnchoDelVisor extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
