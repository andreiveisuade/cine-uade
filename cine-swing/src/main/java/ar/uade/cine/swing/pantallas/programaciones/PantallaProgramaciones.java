package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.programaciones.FuncionPlanificada;
import ar.uade.cine.swing.api.dto.programaciones.PedidoProgramacion;
import ar.uade.cine.swing.api.dto.programaciones.Plan;
import ar.uade.cine.swing.api.dto.programaciones.Programacion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.horaDelDia;
import static ar.uade.cine.swing.comun.Formato.precio;

/**
 * Grillas (CU-03b): una programación genera las funciones de un rango de una vez. Confirmar se habilita solo con una
 * previsualización de estos mismos datos: tocar cualquier campo la invalida, así nunca se confirma algo distinto de lo que se vio.
 */
public final class PantallaProgramaciones extends Pantalla {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas,
                             List<String> idiomas, List<String> proyecciones) {
    }

    private final ApiCartelera apiCartelera;
    private final ApiCatalogos apiCatalogos;
    private final ApiProgramaciones apiProgramaciones;
    private final ApiSalas apiSalas;
    private final Map<Integer, String> titulos = new HashMap<>();
    private final Map<Integer, String> nombresSala = new HashMap<>();
    private final JComboBox<Opcion<Integer>> filtroPelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> filtroSala = new JComboBox<>();
    private final JComboBox<Opcion<String>> filtroEstado = new JComboBox<>();
    private final JLabel conteo = new JLabel(" ");
    private final Tabla<Programacion> tabla = new Tabla<>(
            Columna.<Programacion>de("Película", p -> titulos.getOrDefault(p.peliculaId(), "Película " + p.peliculaId()))
                    .ancho(220),
            Columna.<Programacion>de("Sala", p -> nombresSala.getOrDefault(p.salaId(), "Sala " + p.salaId())),
            Columna.<Programacion>de("Cuándo", PantallaProgramaciones::cuando).ancho(260),
            Columna.<Programacion>de("Días", p -> SelectorDias.resumen(p.diasSemana())),
            Columna.<Programacion>numero("Precio", p -> precio(p.precio())),
            Columna.<Programacion>de("Estado", p -> p.activa() ? "Activa" : "Dada de baja"));
    private final JTextArea detalle = areaDeTexto();
    private final JButton cambiarActivacion = new JButton("Dar de baja");

    private final JComboBox<Opcion<Integer>> pelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JDateChooser hasta = Fechas.selector(null);
    private final JSpinner horaInicio = Fechas.hora(LocalTime.of(20, 30));
    private final SelectorDias dias = new SelectorDias();
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JTextField precioBase = Campos.soloDecimal(new JTextField("5000"));
    private final JLabel error = Componentes.texto(" ");
    private final JButton confirmar = new JButton("Confirmar");
    private final JTextArea informe = areaDeTexto();
    // El plan previsualizado vale solo para los datos con que se pidió.
    private Plan previsualizado;
    private boolean llenando;
    // Cuántas grillas hay sin filtro: se cuenta al entrar y tras crear o dar de baja, no en cada filtro.
    private int total;

    public PantallaProgramaciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiProgramaciones apiProgramaciones,
                           ApiSalas apiSalas) {
        super("Grilla de funciones", "Una grilla genera las funciones del rango de una sola vez. Las que chocan "
                + "con algo ya programado en esa sala se saltean, y el informe dice cuáles.");
        this.apiCartelera = apiCartelera;
        this.apiCatalogos = apiCatalogos;
        this.apiProgramaciones = apiProgramaciones;
        this.apiSalas = apiSalas;

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(barraFiltros(), BorderLayout.NORTH);
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
        add(formulario(), BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tabla.seleccionada().ifPresent(this::verDetalle);
        });
        cargarCatalogos();
    }

    private static JTextArea areaDeTexto() {
        JTextArea area = new JTextArea(6, 30);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static String cuando(Programacion p) {
        String hora = horaDelDia(p.horaInicio());
        if (p.hasta() != null) return p.desde() + " al " + p.hasta() + " · " + hora;
        return p.desde() + " en adelante · " + hora + " (generada hasta "
                + (p.generadaHasta() == null ? "—" : p.generadaHasta()) + ")";
    }

    private JPanel barraFiltros() {
        JPanel barra = new JPanel(new FlujoConSalto());
        filtroPelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        filtroEstado.addItem(new Opcion<>(null, "Todas"));
        filtroEstado.addItem(new Opcion<>("true", "Activas"));
        filtroEstado.addItem(new Opcion<>("false", "Dadas de baja"));
        JButton limpiar = new JButton("Limpiar");
        barra.add(new JLabel("Película"));
        barra.add(filtroPelicula);
        barra.add(new JLabel("Sala"));
        barra.add(filtroSala);
        barra.add(new JLabel("Estado"));
        barra.add(filtroEstado);
        barra.add(limpiar);
        barra.add(conteo);
        filtroPelicula.addActionListener(e -> buscar());
        filtroSala.addActionListener(e -> buscar());
        filtroEstado.addActionListener(e -> buscar());
        limpiar.addActionListener(e -> {
            llenando = true;
            filtroPelicula.setSelectedIndex(0);
            filtroSala.setSelectedIndex(0);
            filtroEstado.setSelectedIndex(0);
            llenando = false;
            recargar();
        });
        return barra;
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

    private JScrollPane formulario() {
        JButton previsualizar = new JButton("Previsualizar");
        previsualizar.addActionListener(e -> previsualizar());
        confirmar.addActionListener(e -> confirmar());
        confirmar.setEnabled(false);
        JPanel botones = new JPanel(new java.awt.GridLayout(1, 2, 6, 0));
        botones.add(previsualizar);
        botones.add(confirmar);

        Formulario formulario = new Formulario()
                .ancho(Componentes.subtitulo("Nueva grilla"))
                .obligatorio("Película", pelicula)
                .obligatorio("Sala", sala)
                .obligatorio("Desde", desde)
                .campo("Hasta", hasta)
                .ancho(Componentes.nota("Hasta vacío = sin fin."))
                .obligatorio("Hora", horaInicio)
                .ancho(new JLabel("Días (ninguno = todos)"))
                .ancho(dias)
                .obligatorio("Idioma", idioma)
                .obligatorio("Proyección", proyeccion)
                .obligatorio("Precio base", precioBase)
                .ancho(botones)
                .ancho(error);
        JScrollPane scrollInforme = new JScrollPane(informe);
        scrollInforme.setPreferredSize(new Dimension(200, 180));
        formulario.ancho(scrollInforme)
                .ancho(Componentes.nota("Al confirmar, el servidor <b>vuelve a revisar</b> cada fecha: entre que mirás "
                        + "el informe y confirmás, otro puede haber programado algo en esa sala."))
                .cerrar();

        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        Runnable invalidar = this::invalidar;
        pelicula.addActionListener(e -> invalidar.run());
        sala.addActionListener(e -> invalidar.run());
        idioma.addActionListener(e -> invalidar.run());
        proyeccion.addActionListener(e -> invalidar.run());
        desde.addPropertyChangeListener("date", e -> invalidar.run());
        hasta.addPropertyChangeListener("date", e -> invalidar.run());
        horaInicio.addChangeListener(e -> invalidar.run());
        dias.alCambiar(invalidar);
        Campos.alCambiar(precioBase, invalidar);

        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    private void invalidar() {
        if (previsualizado == null) return;
        previsualizado = null;
        confirmar.setEnabled(false);
        informe.setText("");
    }

    private void cargarCatalogos() {
        cargar(() -> new Catalogos(apiCartelera.obtenerPeliculas(null), apiSalas.obtenerSalas(),
                apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), c -> {
            llenando = true;
            c.peliculas().forEach(p -> titulos.put(p.id(), p.titulo()));
            c.salas().forEach(s -> nombresSala.put(s.id(), s.nombre()));
            pelicula.removeAllItems();
            c.peliculas().forEach(p -> pelicula.addItem(
                    new Opcion<>(p.id(), p.titulo() + " (" + p.duracionMinutos() + "′)")));
            sala.removeAllItems();
            c.salas().forEach(s -> sala.addItem(new Opcion<>(s.id(), s.nombre() + " — " + etiqueta(s.tipo()))));
            filtroPelicula.removeAllItems();
            filtroPelicula.addItem(new Opcion<>(null, "Todas"));
            c.peliculas().forEach(p -> filtroPelicula.addItem(new Opcion<>(p.id(), p.titulo())));
            filtroSala.removeAllItems();
            filtroSala.addItem(new Opcion<>(null, "Todas"));
            c.salas().forEach(s -> filtroSala.addItem(new Opcion<>(s.id(), s.nombre())));
            idioma.removeAllItems();
            Opcion.de(c.idiomas(), v -> etiqueta(v)).forEach(idioma::addItem);
            proyeccion.removeAllItems();
            Opcion.de(c.proyecciones(), v -> etiqueta(v)).forEach(proyeccion::addItem);
            llenando = false;
            recargar();
        });
    }

    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        Integer peliculaId = Campos.elegido(filtroPelicula);
        Integer salaId = Campos.elegido(filtroSala);
        filtros.put("peliculaId", peliculaId == null ? null : peliculaId.toString());
        filtros.put("salaId", salaId == null ? null : salaId.toString());
        filtros.put("activa", Campos.elegido(filtroEstado));
        return filtros;
    }

    private void recargar() {
        cargar(() -> apiProgramaciones.obtenerProgramaciones(null).size(), cuantas -> {
            total = cuantas;
            buscar();
        });
    }

    private void buscar() {
        if (llenando) return;
        Map<String, String> filtros = filtros();
        cargar(() -> apiProgramaciones.obtenerProgramaciones(filtros), visibles -> {
            tabla.mostrar(visibles);
            conteo.setText(visibles.size() == total ? total + " grillas"
                    : "mostrando " + visibles.size() + " de " + total);
        });
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

    /** El pedido leído del formulario, o null si falta algo o hay algo mal tipeado: entonces no se manda. */
    private PedidoProgramacion pedido(Validacion v) {
        Integer peliculaId = v.elegido(pelicula, "Película");
        Integer salaId = v.elegido(sala, "Sala");
        String inicio = v.fecha(desde, "Desde", true);
        // `hasta` vacío viaja null: es una grilla abierta, no una fecha que falta.
        String fin = v.fecha(hasta, "Hasta", false);
        String idiomaElegido = v.elegido(idioma, "Idioma");
        String proyeccionElegida = v.elegido(proyeccion, "Proyección");
        Double precio = v.decimal(precioBase, "Precio base", true);
        v.alMencionar("rango", desde);
        if (!v.ok()) return null;
        return new PedidoProgramacion(peliculaId, salaId, inicio, fin, Fechas.leerHora(horaInicio).toString(),
                dias.elegidos(), idiomaElegido, proyeccionElegida, precio);
    }

    private void previsualizar() {
        Validacion v = new Validacion(error);
        PedidoProgramacion pedido = pedido(v);
        if (pedido == null) return;
        Tarea.ejecutar(this, () -> apiProgramaciones.previsualizarProgramacion(pedido), plan -> {
            informe.setText(textoDelPlan(plan, false));
            informe.setCaretPosition(0);
            previsualizado = plan;
            confirmar.setEnabled(plan.generadas() > 0);
        }, e -> {
            invalidar();
            v.mostrarError(e);
        });
    }

    private void confirmar() {
        Validacion v = new Validacion(error);
        PedidoProgramacion pedido = pedido(v);
        if (pedido == null) return;
        confirmar.setEnabled(false);
        Tarea.ejecutar(this, () -> apiProgramaciones.crearProgramacion(pedido), plan -> {
            // Se repinta con lo que devolvió el servidor, que revalidó cada fecha al aplicar.
            informe.setText(textoDelPlan(plan, true));
            informe.setCaretPosition(0);
            previsualizado = null;
            avisar("Grilla creada: " + plan.generadas() + " funciones"
                    + (plan.salteadas() > 0 ? ", " + plan.salteadas() + " salteadas" : ""));
            recargar();
        }, e -> {
            confirmar.setEnabled(previsualizado != null);
            v.mostrarError(e);
        });
    }

    private static String textoDelPlan(Plan plan, boolean aplicado) {
        StringBuilder texto = new StringBuilder();
        texto.append(aplicado ? "Se generaron " : "Se van a generar ").append(plan.generadas()).append(" funciones");
        if (plan.salteadas() > 0) {
            texto.append(", ").append(plan.salteadas()).append(aplicado ? " se saltearon" : " se saltean");
        }
        texto.append("\n\n");
        for (FuncionPlanificada f : plan.funciones()) {
            texto.append(fechaHora(f.inicio()));
            if (f.choca()) texto.append("  ✗ ").append(f.motivo() == null ? "se pisa con otra función" : f.motivo());
            texto.append('\n');
        }
        return texto.toString();
    }

}
