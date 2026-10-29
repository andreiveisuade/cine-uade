package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.EstadoImportador;
import ar.uade.cine.swing.api.dto.cartelera.Importacion;
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

    private final ApiCartelera apiCartelera;
    private final JLabel aviso = new JLabel();
    private final ButtonGroup cantidad = new ButtonGroup();
    private final Map<Integer, JRadioButton> paginas = Map.of(
            1, opcion(1), 2, opcion(2), 3, opcion(3));
    private final JButton traer = Componentes.primario(new JButton("Traer cartelera"));
    private final JProgressBar trayendo = new JProgressBar();
    private final JLabel error = Componentes.texto(" ");
    private final Tabla<Importacion> corridas = new Tabla<>(
            Columna.<Importacion>de("Cuándo", c -> fechaHora(c.pedidaEn())).ancho(120),
            Columna.<Importacion>de("Estado", c -> etiqueta(c.estado())),
            Columna.<Importacion>numero("Nuevas", Importacion::nuevas),
            Columna.<Importacion>numero("Ya estaban", Importacion::salteadas),
            Columna.<Importacion>numero("Rechazadas", Importacion::fallidas));
    private final DetalleCorrida detalle;

    public PantallaImportador(ApiCartelera apiCartelera, Navegacion navegacion) {
        super("Importador", "Trae de TMDB las películas que están hoy en cartelera en Argentina. Nada se "
                + "publica: todo cae en Por revisar y espera que alguien lo confirme.");
        this.apiCartelera = apiCartelera;
        this.detalle = new DetalleCorrida(() -> navegacion.ir(Destino.POR_REVISAR));

        trayendo.setIndeterminate(true);
        trayendo.setString("Preguntándole a TMDB qué se está dando, y cargando lo que falte. Son unos segundos.");
        trayendo.setStringPainted(true);
        trayendo.setVisible(false);
        aviso.setForeground(Colores.aviso());
        aviso.setVisible(false);

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

        JSplitPane partes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listado, detalle);
        partes.setResizeWeight(0.3);
        partes.setDividerLocation(470);
        partes.setBorder(null);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(partes, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        corridas.tabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) detalle.mostrar(corridas.seleccionada().orElse(null));
        });
        traer.addActionListener(e -> traer());
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

    private static JRadioButton opcion(int paginas) {
        return new JRadioButton(String.valueOf(paginas * DetalleCorrida.POR_PAGINA));
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
            if (corrida.fallo()) {
                Mensajes.error(this, "La importación falló: " + corrida.motivoDelFallo());
            } else {
                avisar(corrida.nuevas() == 0 ? "No había nada nuevo en TMDB"
                        : DetalleCorrida.resumen(corrida) + ": las nuevas están en Por revisar");
            }
            recargar(corrida.id());
        }, e -> {
            // Un 400 (la anterior terminó recién) va junto al botón; un 409 (otra en curso), en pop-up.
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
}
