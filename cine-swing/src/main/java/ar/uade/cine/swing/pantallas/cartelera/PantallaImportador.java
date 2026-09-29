package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.cartelera.EstadoImportador;
import ar.uade.cine.swing.api.dto.cartelera.Importacion;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

/** Trae de TMDB lo que está en cartelera en Argentina. Nada se publica: todo cae en Por revisar. */
public final class PantallaImportador extends Pantalla {

    private record Datos(List<Importacion> corridas, EstadoImportador estado) {
    }

    private final ApiCartelera apiCartelera;
    private final JLabel aviso = new JLabel();
    private final JComboBox<Opcion<Integer>> paginas = new JComboBox<>();
    private final JButton traer = new JButton("Traer cartelera");
    private final JProgressBar trayendo = new JProgressBar();
    private final JLabel error = Componentes.texto(" ");
    private final Tabla<Importacion> tabla = new Tabla<>(
            Columna.<Importacion>de("Cuándo", c -> fechaHora(c.pedidaEn())).ancho(130),
            Columna.<Importacion>de("Estado", c -> etiqueta(c.estado())),
            Columna.<Importacion>numero("Nuevas", Importacion::nuevas),
            Columna.<Importacion>numero("Salteadas", Importacion::salteadas),
            Columna.<Importacion>numero("Fallidas", Importacion::fallidas));
    // Casi nunca se mira, pero es lo único que dice por qué una película no entró.
    private final JTextArea detalle = new JTextArea();

    public PantallaImportador(ApiCartelera apiCartelera, Navegacion navegacion) {
        super("Importador", "Trae de TMDB las películas que están hoy en cartelera en Argentina. Nada se "
                + "publica: todo cae en Por revisar y espera que alguien lo confirme.");
        this.apiCartelera = apiCartelera;

        paginas.addItem(new Opcion<>(1, "Una página (20 títulos)"));
        paginas.addItem(new Opcion<>(2, "Dos páginas (40 títulos)"));
        paginas.addItem(new Opcion<>(3, "Tres páginas (60 títulos)"));
        trayendo.setIndeterminate(true);
        trayendo.setString("Preguntándole a TMDB qué se está dando, y cargando lo que falte. Son unos segundos.");
        trayendo.setStringPainted(true);
        trayendo.setVisible(false);
        aviso.setForeground(Colores.aviso());
        aviso.setVisible(false);
        JButton porRevisar = new JButton("Ir a Por revisar");
        porRevisar.addActionListener(e -> navegacion.ir(Destino.POR_REVISAR));

        JPanel controles = new JPanel(new FlujoConSalto());
        controles.add(new JLabel("Cuánto traer *"));
        controles.add(paginas);
        controles.add(traer);
        controles.add(porRevisar);
        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.add(Componentes.izquierda(aviso));
        arriba.add(Componentes.izquierda(controles));
        arriba.add(Componentes.izquierda(error));
        arriba.add(Componentes.izquierda(trayendo));

        detalle.setEditable(false);
        detalle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detalle.setLineWrap(true);
        tabla.tabla().getSelectionModel().addListSelectionListener(e ->
                detalle.setText(tabla.seleccionada().map(c -> c.detalle() == null ? "—" : c.detalle()).orElse("")));
        JPanel listado = new JPanel(new BorderLayout(0, 4));
        listado.add(Componentes.subtitulo("Corridas anteriores"), BorderLayout.NORTH);
        listado.add(tabla.conScroll());
        JPanel panelDetalle = new JPanel(new BorderLayout(0, 4));
        panelDetalle.add(new JLabel("Detalle de la corrida elegida (+ al buzón, ✗ rechazada)"), BorderLayout.NORTH);
        panelDetalle.add(new JScrollPane(detalle));
        JSplitPane partes = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listado, panelDetalle);
        partes.setResizeWeight(0.55);
        partes.setBorder(null);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(partes, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        traer.addActionListener(e -> traer());
        recargar(null);
    }

    private void recargar(Integer destacada) {
        cargar(() -> new Datos(apiCartelera.obtenerImportaciones(), apiCartelera.estadoImportador()), datos -> {
            aviso.setText("El importador no está disponible: " + datos.estado().detalle());
            aviso.setVisible(!datos.estado().disponible());
            traer.setEnabled(datos.estado().disponible());
            tabla.mostrar(datos.corridas());
            if (destacada != null) {
                for (int i = 0; i < datos.corridas().size(); i++) {
                    if (datos.corridas().get(i).id() == destacada) tabla.tabla().setRowSelectionInterval(i, i);
                }
            }
        });
    }

    // Sin consultas repetidas: la corrida tarda unos quince segundos y su respuesta ya trae los contadores.
    private void traer() {
        Validacion v = new Validacion(error);
        Integer cuantas = v.elegido(paginas, "Cuánto traer");
        if (!v.ok()) return;
        traer.setEnabled(false);
        paginas.setEnabled(false);
        trayendo.setVisible(true);
        Tarea.ejecutar(this, () -> apiCartelera.importarAhora(cuantas), corrida -> {
            terminar();
            if ("FALLIDA".equals(corrida.estado())) {
                Mensajes.error(this, new ErrorApi(-1, resumen(corrida)));
            } else {
                avisar(resumen(corrida));
            }
            recargar(corrida.id());
        }, e -> {
            // Un 400 (ya hay una corriendo) es una respuesta, no una pantalla rota: va junto al botón.
            terminar();
            traer.setEnabled(true);
            v.mostrarError(e);
        });
    }

    private void terminar() {
        trayendo.setVisible(false);
        paginas.setEnabled(true);
    }

    private static String resumen(Importacion corrida) {
        if ("FALLIDA".equals(corrida.estado())) {
            return corrida.detalle() != null ? corrida.detalle() : "La importación falló";
        }
        if (corrida.nuevas() == 0) return "No había nada nuevo en TMDB";
        String plural = corrida.nuevas() == 1 ? "" : "s";
        return corrida.nuevas() + " película" + plural + " nueva" + plural + " en Por revisar";
    }
}
