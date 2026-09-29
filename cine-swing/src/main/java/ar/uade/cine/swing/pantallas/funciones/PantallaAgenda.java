package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;
import com.toedter.calendar.JCalendar;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

/**
 * La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura, así se ve si dos funciones se
 * pisan (R3) y dónde entra algo nuevo. Dos modos: una sala toda la semana, o todas las salas un día; en una sola
 * columna, las funciones simultáneas de varias salas se pisarían.
 */
public final class PantallaAgenda extends Pantalla {

    private final ApiFunciones apiFunciones;
    private final ApiSalas apiSalas;
    private final JComboBox<Opcion<Boolean>> modo = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JLabel etiquetaSala = new JLabel("Sala");
    private final JLabel conteo = new JLabel(" ");
    private final JCalendar calendario = Fechas.calendario(LocalDate.now());
    private final GrillaAgenda grilla;
    private final JLabel vacio = new JLabel("No hay funciones programadas en este rango.", JLabel.CENTER);
    private final JPanel lienzo = new JPanel(new BorderLayout());
    private LocalDate desde = LocalDate.now();
    private List<Sala> salas = List.of();
    // Cada pedido lleva el número de la vista que lo pidió: una respuesta lenta de una semana anterior no pisa la actual.
    private int vista;
    // Mover el calendario desde el código dispara su propio evento: sin esta marca, se redibujaría dos veces.
    private boolean sincronizando = true;

    public PantallaAgenda(ApiFunciones apiFunciones, ApiSalas apiSalas, Navegacion navegacion) {
        super("Agenda", "La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura. "
                + "Los huecos son dónde entra algo nuevo. Clic en un bloque para ver su borderó e informe.");
        this.apiFunciones = apiFunciones;
        this.apiSalas = apiSalas;
        this.grilla = new GrillaAgenda(navegacion);

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
                + "duran menos de "
                + Math.round(GrillaAgenda.ALTO_MINIMO / GrillaAgenda.PX_POR_MINUTO) + " minutos se dibujan con un alto "
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

        cargar(apiSalas::obtenerSalas, lista -> {
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
        List<ColumnaAgenda> columnas = new ArrayList<>();
        if (!semana) {
            for (Sala s : salas) {
                columnas.add(new ColumnaAgenda(s.nombre(), etiqueta(s.tipo()),
                        f -> f.sala().id() == s.id() && dia(f).equals(primerDia), f -> etiqueta(f.proyeccion())));
            }
        } else if (elegida != null) {
            for (int i = 0; i < 7; i++) {
                LocalDate fecha = primerDia.plusDays(i);
                columnas.add(new ColumnaAgenda(SelectorDias.abreviatura(fecha.getDayOfWeek()),
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
        cargar(() -> apiFunciones.obtenerFunciones(filtros), funciones -> {
            if (pedida == vista) dibujar(columnas, funciones, donde);
        });
    }

    private void dibujar(List<ColumnaAgenda> columnas, List<Funcion> funciones, String donde) {
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

}
