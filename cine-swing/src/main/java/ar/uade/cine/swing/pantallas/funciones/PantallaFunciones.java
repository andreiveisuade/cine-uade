package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/**
 * Listado con filtros y alta de funciones. No anticipa R3 ni R8 antes de mandar: el
 * backend las valida igual y su mensaje llega tal cual, así la regla vive en un solo lugar.
 */
public final class PantallaFunciones extends Pantalla {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas,
                             List<String> proyecciones) {
    }

    private final ApiCartelera apiCartelera;
    private final ApiCatalogos apiCatalogos;
    private final ApiFunciones apiFunciones;
    private final ApiSalas apiSalas;
    private final Navegacion navegacion;
    private final JComboBox<Opcion<Integer>> filtroPelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> filtroSala = new JComboBox<>();
    private final JDateChooser desde = Fechas.selector(null);
    private final JDateChooser hasta = Fechas.selector(null);
    private final JLabel conteo = new JLabel(" ");
    private final Tabla<Funcion> tabla = new Tabla<>(
            Columna.<Funcion>de("Cuándo", f -> dia(f.inicio()) + "  " + hora(f.inicio())).ancho(140),
            Columna.<Funcion>de("Película", f -> f.pelicula().titulo()).ancho(260),
            Columna.<Funcion>de("Sala", f -> f.sala().nombre()),
            Columna.<Funcion>de("Formato", f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())),
            Columna.<Funcion>numero("Precio", f -> precio(f.precio())));

    private final JComboBox<Opcion<Integer>> pelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JDateChooser dia = Fechas.selector(LocalDate.now());
    private final JSpinner hora = Fechas.hora(LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1));
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JTextField precioBase = Campos.soloDecimal(new JTextField());
    private final JLabel error = Componentes.texto(" ");
    // Cuántas hay sin filtro, para el "mostrando 3 de 40": se cuenta al entrar y tras cada alta o baja.
    private int total;
    // Mientras se llenan los combos no hay que disparar búsquedas.
    private boolean llenando;

    public PantallaFunciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiFunciones apiFunciones,
                      ApiSalas apiSalas, Navegacion navegacion) {
        super("Funciones", "Es la lista más larga del panel: una semana de seis salas pasa de cien funciones. "
                + "Doble clic en una función abre su borderó e informe.");
        this.apiCartelera = apiCartelera;
        this.apiCatalogos = apiCatalogos;
        this.apiFunciones = apiFunciones;
        this.apiSalas = apiSalas;
        this.navegacion = navegacion;

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(barraFiltros(), BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(accionesDeFila(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(formulario(), BorderLayout.EAST);

        tabla.alDobleClic(this::abrirInformes);
        cargarCatalogos();
    }

    private JPanel barraFiltros() {
        JPanel barra = new JPanel(new FlujoConSalto());
        filtroPelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        barra.add(new JLabel("Película"));
        barra.add(filtroPelicula);
        barra.add(new JLabel("Sala"));
        barra.add(filtroSala);
        barra.add(new JLabel("Desde"));
        barra.add(desde);
        barra.add(new JLabel("Hasta"));
        barra.add(hasta);
        JButton buscar = new JButton("Buscar");
        JButton limpiar = new JButton("Limpiar");
        barra.add(buscar);
        barra.add(limpiar);
        barra.add(conteo);

        filtroPelicula.addActionListener(e -> buscar());
        filtroSala.addActionListener(e -> buscar());
        desde.addPropertyChangeListener("date", e -> buscar());
        hasta.addPropertyChangeListener("date", e -> buscar());
        buscar.addActionListener(e -> buscar());
        limpiar.addActionListener(e -> {
            llenando = true;
            filtroPelicula.setSelectedIndex(0);
            filtroSala.setSelectedIndex(0);
            desde.setDate(null);
            hasta.setDate(null);
            llenando = false;
            recargar();
        });
        return barra;
    }

    private JPanel accionesDeFila() {
        JPanel acciones = new JPanel(new FlujoConSalto());
        JButton informes = new JButton("Borderó e informe");
        JButton borrar = new JButton("Borrar");
        informes.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirInformes));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::borrar));
        acciones.add(informes);
        acciones.add(borrar);
        return acciones;
    }

    private JScrollPane formulario() {
        JButton programar = new JButton("Programar");
        programar.addActionListener(e -> programar());

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(Componentes.subtitulo("Programar función"))
                .obligatorio("Película", pelicula)
                .obligatorio("Sala", sala)
                .obligatorio("Día", dia)
                .obligatorio("Hora", hora)
                .obligatorio("Idioma", idioma)
                .obligatorio("Proyección", proyeccion)
                .obligatorio("Precio base", precioBase)
                .ancho(programar)
                .ancho(error)
                .cerrar();
        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    private void cargarCatalogos() {
        cargar(() -> new Catalogos(apiCartelera.obtenerPeliculas(null), apiSalas.obtenerSalas(),
                apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), catalogos -> {
            llenando = true;
            llenar(pelicula, catalogos.peliculas().stream()
                    .map(p -> new Opcion<>(p.id(), p.titulo() + " (" + p.duracionMinutos() + "′)")).toList(), false);
            llenar(filtroPelicula, catalogos.peliculas().stream()
                    .map(p -> new Opcion<>(p.id(), p.titulo())).toList(), true);
            llenar(sala, catalogos.salas().stream()
                    .map(s -> new Opcion<>(s.id(), s.nombre() + " — " + etiqueta(s.tipo()))).toList(), false);
            llenar(filtroSala, catalogos.salas().stream().map(s -> new Opcion<>(s.id(), s.nombre())).toList(), true);
            idioma.removeAllItems();
            Opcion.de(catalogos.idiomas(), v -> etiqueta(v)).forEach(idioma::addItem);
            proyeccion.removeAllItems();
            Opcion.de(catalogos.proyecciones(), v -> etiqueta(v)).forEach(proyeccion::addItem);
            llenando = false;
            recargar();
        });
    }

    private static void llenar(JComboBox<Opcion<Integer>> combo, List<Opcion<Integer>> opciones, boolean conTodas) {
        combo.removeAllItems();
        if (conTodas) combo.addItem(new Opcion<>(null, "Todas"));
        opciones.forEach(combo::addItem);
    }

    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        Integer peliculaId = Campos.elegido(filtroPelicula);
        Integer salaId = Campos.elegido(filtroSala);
        filtros.put("peliculaId", peliculaId == null ? null : peliculaId.toString());
        filtros.put("salaId", salaId == null ? null : salaId.toString());
        filtros.put("desde", Fechas.iso(desde));
        filtros.put("hasta", Fechas.iso(hasta));
        return filtros;
    }

    private void recargar() {
        cargar(() -> apiFunciones.obtenerFunciones(null).size(), cuantas -> {
            total = cuantas;
            buscar();
        });
    }

    private void buscar() {
        if (llenando) return;
        Map<String, String> filtros = filtros();
        cargar(() -> apiFunciones.obtenerFunciones(filtros), visibles -> {
            tabla.mostrar(visibles);
            conteo.setText(visibles.size() == total
                    ? total + " programadas"
                    : "mostrando " + visibles.size() + " de " + total);
        });
    }

    private void programar() {
        Validacion v = new Validacion(error);
        Integer peliculaId = v.elegido(pelicula, "Película");
        Integer salaId = v.elegido(sala, "Sala");
        String elegido = v.fecha(dia, "Día", true);
        String idiomaElegido = v.elegido(idioma, "Idioma");
        String proyeccionElegida = v.elegido(proyeccion, "Proyección");
        Double precio = v.decimal(precioBase, "Precio base", true);
        if (!v.ok()) return;
        String inicio = Fechas.isoCompleto(LocalDate.parse(elegido).atTime(Fechas.leerHora(hora)));
        PedidoFuncion pedido = new PedidoFuncion(peliculaId, salaId, inicio, idiomaElegido, proyeccionElegida, precio);
        Tarea.ejecutar(this, () -> apiFunciones.programarFuncion(pedido), creada -> {
            avisar("Función programada");
            recargar();
        }, v::mostrarError);
    }

    private void borrar(Funcion funcion) {
        if (!confirmar("¿Borrar la función de " + funcion.pelicula().titulo() + " del " + dia(funcion.inicio())
                + " " + hora(funcion.inicio()) + "?", "Sí, borrar")) return;
        accion(() -> {
            apiFunciones.eliminarFuncion(funcion.id());
            return null;
        }, "Función borrada", this::recargar);
    }

    private void abrirInformes(Funcion funcion) {
        navegacion.abrirInforme(funcion.id());
    }
}
