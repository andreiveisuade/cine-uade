package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Funcion;
import ar.uade.cine.swing.api.dto.PedidoFuncion;
import ar.uade.cine.swing.api.dto.Pelicula;
import ar.uade.cine.swing.api.dto.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
 * Listado con filtros y alta de funciones. A diferencia del panel web, no anticipa R3 ni R8 antes de mandar: el
 * backend las valida igual y su mensaje llega tal cual, así la regla vive en un solo lugar.
 */
final class PantallaFunciones extends Pantalla {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas,
                             List<String> proyecciones) {
    }

    private record Resultado(int total, List<Funcion> visibles) {
    }

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
    private final JTextField precioBase = new JTextField();
    // Mientras se llenan los combos no hay que disparar búsquedas.
    private boolean llenando;

    PantallaFunciones(ApiHttp api, Navegacion navegacion) {
        super(api, "Funciones", "Es la lista más larga del panel: una semana de seis salas pasa de cien funciones. "
                + "Doble clic en una función abre su borderó e informe.");
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
            buscar();
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

    private JPanel formulario() {
        JButton programar = new JButton("Programar");
        programar.addActionListener(e -> programar());

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(Componentes.subtitulo("Programar función"))
                .campo("Película", pelicula)
                .campo("Sala", sala)
                .campo("Día", dia)
                .campo("Hora", hora)
                .campo("Idioma", idioma)
                .campo("Proyección", proyeccion)
                .campo("Precio base", precioBase)
                .ancho(programar)
                .cerrar();
        JPanel panel = Componentes.conBorde(formulario);
        panel.setPreferredSize(new Dimension(340, 0));
        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        return panel;
    }

    private void cargarCatalogos() {
        cargar(() -> new Catalogos(api.obtenerPeliculas(null), api.obtenerSalas(), api.obtenerIdiomas(),
                api.obtenerProyecciones()), catalogos -> {
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
            buscar();
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

    private void buscar() {
        if (llenando) return;
        Map<String, String> filtros = filtros();
        // El total sin filtro arma el "mostrando 3 de 40", que avisa que hay un filtro puesto.
        cargar(() -> new Resultado(api.obtenerFunciones(null).size(), api.obtenerFunciones(filtros)), r -> {
            tabla.mostrar(r.visibles());
            conteo.setText(r.visibles().size() == r.total()
                    ? r.total() + " programadas"
                    : "mostrando " + r.visibles().size() + " de " + r.total());
        });
    }

    private void programar() {
        LocalDate elegido = Fechas.leer(dia);
        String inicio = elegido == null ? null : Fechas.isoCompleto(elegido.atTime(Fechas.leerHora(hora)));
        PedidoFuncion pedido = new PedidoFuncion(Campos.elegido(pelicula), Campos.elegido(sala), inicio,
                Campos.elegido(idioma), Campos.elegido(proyeccion),
                Campos.decimal(precioBase));
        accion(() -> api.programarFuncion(pedido), "Función programada", this::buscar);
    }

    private void borrar(Funcion funcion) {
        if (!confirmar("¿Borrar la función de " + funcion.pelicula().titulo() + " del " + dia(funcion.inicio())
                + " " + hora(funcion.inicio()) + "?")) return;
        accion(() -> {
            api.eliminarFuncion(funcion.id());
            return null;
        }, "Función borrada", this::buscar);
    }

    private void abrirInformes(Funcion funcion) {
        navegacion.abrir(new PantallaFuncion(api, navegacion, funcion.id()));
    }
}
