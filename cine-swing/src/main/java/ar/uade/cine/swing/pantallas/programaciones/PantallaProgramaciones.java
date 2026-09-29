package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.programaciones.Programacion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.PantallaListado;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.horaDelDia;
import static ar.uade.cine.swing.comun.Formato.precio;

// Grillas (CU-03b): el listado con su detalle y baja, y el alta al costado (FormularioProgramacion).
/** Una grilla genera las funciones de un rango de una vez; las que chocan se saltean y el informe dice cuáles. */
public final class PantallaProgramaciones extends PantallaListado<Programacion> {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas,
                             List<String> idiomas, List<String> proyecciones) {
    }

    private final ApiProgramaciones apiProgramaciones;
    private final Map<Integer, String> titulos = new HashMap<>();
    private final Map<Integer, String> nombresSala = new HashMap<>();
    private final JComboBox<Opcion<Integer>> filtroPelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> filtroSala = new JComboBox<>();
    private final Tabla<Programacion> tabla = new Tabla<>(
            Columna.<Programacion>de("Película", p -> titulos.getOrDefault(p.peliculaId(),
                    "Película " + p.peliculaId())).ancho(220),
            Columna.<Programacion>de("Sala", p -> nombresSala.getOrDefault(p.salaId(), "Sala " + p.salaId())),
            Columna.<Programacion>de("Cuándo", PantallaProgramaciones::cuando).ancho(260),
            Columna.<Programacion>de("Días", p -> SelectorDias.resumen(p.diasSemana())),
            Columna.<Programacion>numero("Precio", p -> precio(p.precio())),
            Columna.<Programacion>de("Estado", p -> p.activa() ? "Activa" : "Dada de baja"));
    private final JTextArea detalle = Componentes.areaDeLectura(6, 30);
    private final JButton cambiarActivacion = new JButton("Dar de baja");
    private final FormularioProgramacion formulario;

    public PantallaProgramaciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos,
                                  ApiProgramaciones apiProgramaciones, ApiSalas apiSalas) {
        super("Grilla de funciones", "Una grilla genera las funciones del rango de una sola vez. Las que chocan "
                + "con algo ya programado en esa sala se saltean, y el informe dice cuáles.");
        this.apiProgramaciones = apiProgramaciones;
        this.formulario = new FormularioProgramacion(apiProgramaciones, this::recargar);

        filtroPelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        JComboBox<Opcion<String>> filtroEstado = new JComboBox<>();
        filtroEstado.addItem(new Opcion<>(null, "Todas"));
        filtroEstado.addItem(new Opcion<>("true", "Activas"));
        filtroEstado.addItem(new Opcion<>("false", "Dadas de baja"));
        filtros.combo("Película", "peliculaId", filtroPelicula)
                .combo("Sala", "salaId", filtroSala)
                .combo("Estado", "activa", filtroEstado)
                .limpiar(this::recargar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(filtros, BorderLayout.NORTH);
        JPanel listado = new JPanel(new BorderLayout(0, 6));
        listado.add(tabla.conScroll(), BorderLayout.CENTER);
        listado.add(accionesDeFila(), BorderLayout.SOUTH);
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(Componentes.nota("Dar de baja una grilla <b>no borra las funciones que ya generó</b>: pueden tener "
                + "entradas vendidas. Solo evita que genere nuevas. Elegí una fila para ver qué funciones creó."),
                BorderLayout.NORTH);
        abajo.add(new JScrollPane(detalle), BorderLayout.CENTER);
        JSplitPane partido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listado, abajo);
        partido.setResizeWeight(0.7);
        partido.setBorder(null);
        centro.add(partido, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(formulario, BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tabla.seleccionada().ifPresent(this::verDetalle);
        });
        cargar(() -> new Catalogos(apiCartelera.obtenerPeliculas(null), apiSalas.obtenerSalas(),
                apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), c -> {
            c.peliculas().forEach(p -> titulos.put(p.id(), p.titulo()));
            c.salas().forEach(s -> nombresSala.put(s.id(), s.nombre()));
            formulario.llenar(c.peliculas(), c.salas(), c.idiomas(), c.proyecciones());
            filtros.enSilencio(() -> {
                Campos.llenarConTodas(filtroPelicula, "Todas", Opciones.peliculas(c.peliculas()));
                Campos.llenarConTodas(filtroSala, "Todas", Opciones.salas(c.salas()));
            });
            recargar();
        });
    }

    @Override
    protected List<Programacion> obtener(Map<String, String> filtros) {
        return apiProgramaciones.obtenerProgramaciones(filtros);
    }

    @Override
    protected Tabla<Programacion> tabla() {
        return tabla;
    }

    @Override
    protected String sinFiltrar(List<Programacion> todas) {
        return todas.size() + " grillas";
    }

    private static String cuando(Programacion p) {
        String hora = horaDelDia(p.horaInicio());
        if (p.hasta() != null) return p.desde() + " al " + p.hasta() + " · " + hora;
        return p.desde() + " en adelante · " + hora + " (generada hasta "
                + (p.generadaHasta() == null ? "—" : p.generadaHasta()) + ")";
    }

    private JPanel accionesDeFila() {
        JPanel acciones = new JPanel(new FlujoConSalto());
        cambiarActivacion.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (p.activa() && !confirmar("¿Dar de baja esta grilla? Deja de generar funciones; las que ya generó "
                    + "quedan.", "Sí, dar de baja")) return;
            accion(() -> apiProgramaciones.cambiarActivacionProgramacion(p.id(), !p.activa()),
                    p.activa() ? "Grilla dada de baja" : "Grilla reactivada", this::recargar);
        }));
        acciones.add(cambiarActivacion);
        return acciones;
    }

    private void verDetalle(Programacion elegida) {
        cambiarActivacion.setText(elegida.activa() ? "Dar de baja" : "Reactivar");
        cargar(() -> apiProgramaciones.obtenerProgramacion(elegida.id()), grilla -> {
            if (grilla.funciones() == null || grilla.funciones().isEmpty()) {
                detalle.setText("Esta grilla no generó ninguna función: todas sus fechas chocaban con algo ya "
                        + "programado.");
                return;
            }
            detalle.setText("Funciones de la grilla " + grilla.id() + " (" + grilla.funciones().size() + "):\n"
                    + grilla.funciones().stream().map(f -> fechaHora(f.inicio())).collect(Collectors.joining("   ")));
            detalle.setCaretPosition(0);
        });
    }
}
