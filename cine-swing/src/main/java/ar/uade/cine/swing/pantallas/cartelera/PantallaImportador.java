package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.EstadoImportador;
import ar.uade.cine.swing.api.dto.cartelera.Importacion;
import ar.uade.cine.swing.api.dto.cartelera.PeliculaDeCorrida;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

// Trae de TMDB lo que está en cartelera en Argentina; nada se publica, todo cae en Por revisar.
public final class PantallaImportador extends Pantalla {

    private record Datos(List<Importacion> corridas, EstadoImportador estado) {
    }

    private static final int POR_PAGINA = 20;

    private final ApiCartelera apiCartelera;
    private final JLabel aviso = new JLabel();
    private final ButtonGroup cantidad = new ButtonGroup();
    private final Map<Integer, JRadioButton> paginas = Map.of(
            1, new JRadioButton("20"), 2, new JRadioButton("40"), 3, new JRadioButton("60"));
    private final JButton traer = new JButton("Traer cartelera");
    private final JProgressBar trayendo = new JProgressBar();
    private final JLabel error = Componentes.texto(" ");
    private final Tabla<Importacion> corridas = new Tabla<>(
            Columna.<Importacion>de("Cuándo", c -> fechaHora(c.pedidaEn())).ancho(120),
            Columna.<Importacion>de("Estado", c -> etiqueta(c.estado())),
            Columna.<Importacion>numero("Nuevas", Importacion::nuevas),
            Columna.<Importacion>numero("Ya estaban", Importacion::salteadas),
            Columna.<Importacion>numero("Rechazadas", Importacion::fallidas));
    private final JLabel tituloDetalle = Componentes.subtitulo(" ");
    private final JLabel resumenDetalle = Componentes.texto(" ");
    private final JButton revisar = new JButton();
    private final Tabla<PeliculaDeCorrida> peliculas = new Tabla<>(
            Columna.<PeliculaDeCorrida>de("Resultado", p -> etiqueta(p.resultado())).ancho(90),
            Columna.<PeliculaDeCorrida>de("Película", PeliculaDeCorrida::titulo).ancho(280),
            Columna.<PeliculaDeCorrida>de("Qué pasó", PantallaImportador::quePaso).ancho(300));

    public PantallaImportador(ApiCartelera apiCartelera, Navegacion navegacion) {
        super("Importador", "Trae de TMDB las películas que están hoy en cartelera en Argentina. Nada se "
                + "publica: todo cae en Por revisar y espera que alguien lo confirme.");
        this.apiCartelera = apiCartelera;

        trayendo.setIndeterminate(true);
        trayendo.setString("Preguntándole a TMDB qué se está dando, y cargando lo que falte. Son unos segundos.");
        trayendo.setStringPainted(true);
        trayendo.setVisible(false);
        aviso.setForeground(Colores.aviso());
        aviso.setVisible(false);
        revisar.setVisible(false);
        revisar.addActionListener(e -> navegacion.ir(Destino.POR_REVISAR));

        JPanel controles = new JPanel(new FlujoConSalto());
        controles.add(new JLabel("Cuántas traer"));
        for (int i = 1; i <= paginas.size(); i++) {
            cantidad.add(paginas.get(i));
            controles.add(paginas.get(i));
        }
        controles.add(new JLabel("películas"));
        controles.add(traer);
        paginas.get(1).setSelected(true);
        Pila arriba = new Pila();
        arriba.agregar(aviso, controles,
                Componentes.nota("Las más populares de las que hoy están en los cines de Argentina, según TMDB. Las que ya están "
                        + "cargadas, aunque las hayas descartado, no se vuelven a traer."),
                error, trayendo);

        JPanel listado = new JPanel(new BorderLayout(0, 6));
        listado.add(Componentes.subtitulo("Corridas"), BorderLayout.NORTH);
        listado.add(corridas.conScroll());

        JPanel encabezadoDetalle = new JPanel(new BorderLayout(12, 0));
        encabezadoDetalle.add(new Pila().agregar(tituloDetalle, resumenDetalle), BorderLayout.CENTER);
        encabezadoDetalle.add(revisar, BorderLayout.EAST);
        JPanel detalle = new JPanel(new BorderLayout(0, 6));
        detalle.add(encabezadoDetalle, BorderLayout.NORTH);
        detalle.add(peliculas.conScroll());

        JSplitPane partes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listado, detalle);
        partes.setResizeWeight(0.3);
        partes.setDividerLocation(470);
        partes.setBorder(null);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(partes, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        corridas.tabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrar(corridas.seleccionada().orElse(null));
        });
        traer.addActionListener(e -> traer());
        mostrar(null);
        recargar(null);
    }

    private void recargar(Integer destacada) {
        cargar(() -> new Datos(apiCartelera.obtenerImportaciones(), apiCartelera.estadoImportador()), datos -> {
            aviso.setText("El importador no está disponible: " + datos.estado().detalle());
            aviso.setVisible(!datos.estado().disponible());
            traer.setEnabled(datos.estado().disponible());
            corridas.mostrar(datos.corridas());
            int fila = 0;
            for (int i = 0; i < datos.corridas().size(); i++) {
                if (destacada != null && datos.corridas().get(i).id() == destacada) fila = i;
            }
            if (!datos.corridas().isEmpty()) corridas.tabla().setRowSelectionInterval(fila, fila);
        });
    }

    private void mostrar(Importacion corrida) {
        if (corrida == null) {
            tituloDetalle.setText("Todavía no se trajo nada");
            resumenDetalle.setText("Elegí cuántas películas y apretá Traer cartelera.");
            resumenDetalle.setForeground(Colores.secundario());
            revisar.setVisible(false);
            peliculas.mostrar(List.of());
            return;
        }
        tituloDetalle.setText("Corrida del " + fechaHora(corrida.pedidaEn()));
        boolean fallo = "FALLIDA".equals(corrida.estado());
        resumenDetalle.setText(fallo ? "Falló: " + (corrida.detalle() == null ? "sin motivo" : corrida.detalle())
                : "Pediste hasta " + corrida.paginas() * POR_PAGINA + ". " + resumen(corrida) + ".");
        resumenDetalle.setForeground(fallo ? Colores.error() : Colores.texto());
        long pendientes = corrida.peliculas().stream().filter(p -> "PENDIENTE".equals(p.estadoRevision())).count();
        revisar.setText("Revisar " + (pendientes == 1 ? "la pendiente" : "las " + pendientes + " pendientes"));
        revisar.setVisible(pendientes > 0);
        peliculas.mostrar(corrida.peliculas());
    }

    // Sin consultas repetidas: la corrida tarda unos quince segundos y su respuesta ya trae los contadores.
    private void traer() {
        Validacion v = new Validacion(error);
        int cuantas = paginas.entrySet().stream().filter(e -> e.getValue().isSelected())
                .mapToInt(Map.Entry::getKey).findFirst().orElse(1);
        habilitar(false);
        trayendo.setVisible(true);
        Tarea.ejecutar(this, () -> apiCartelera.importarAhora(cuantas), corrida -> {
            terminar();
            if ("FALLIDA".equals(corrida.estado())) {
                Mensajes.error(this, corrida.detalle() != null ? corrida.detalle() : "La importación falló");
            } else {
                avisar(corrida.nuevas() == 0 ? "No había nada nuevo en TMDB"
                        : resumen(corrida) + ": las nuevas están en Por revisar");
            }
            recargar(corrida.id());
        }, e -> {
            // Un 400 (ya hay una corriendo) es una respuesta, no una pantalla rota: va junto al botón.
            terminar();
            v.mostrarError(e);
        });
    }

    private void terminar() {
        trayendo.setVisible(false);
        habilitar(true);
    }

    private void habilitar(boolean si) {
        traer.setEnabled(si);
        paginas.values().forEach(b -> b.setEnabled(si));
    }

    private static String resumen(Importacion corrida) {
        return cuenta(corrida.nuevas(), "nueva", "nuevas") + ", "
                + cuenta(corrida.salteadas(), "ya estaba", "ya estaban") + ", "
                + cuenta(corrida.fallidas(), "rechazada", "rechazadas");
    }

    private static String cuenta(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static String quePaso(PeliculaDeCorrida p) {
        return switch (p.resultado()) {
            case "NUEVA" -> p.estadoRevision() == null ? "Entró, después se borró"
                    : "Entró · hoy: " + etiqueta(p.estadoRevision());
            case "YA_ESTABA" -> "Ya estaba cargada: no se tocó";
            default -> p.motivo() == null ? "No pasó las reglas del cine" : p.motivo();
        };
    }
}
