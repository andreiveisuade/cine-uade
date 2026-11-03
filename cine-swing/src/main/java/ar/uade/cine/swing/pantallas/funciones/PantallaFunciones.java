package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.FuncionRechazada;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.PantallaListado;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Listado con filtros de las funciones, con el alta y la edición al costado; doble clic abre su borderó e informe.
// Se eligen varias para borrarlas juntas. Sin anticipar R3, R8 ni R12: el backend valida y su mensaje va tal cual.
public final class PantallaFunciones extends PantallaListado<Funcion> {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas,
                             List<String> proyecciones) {
    }

    private final ApiFunciones apiFunciones;
    private final Navegacion navegacion;
    private final BorrarFunciones borrar;
    private final Predicate<Pregunta> confirmar;
    private final Consumer<String> rechazo;
    // Las que el backend no dejó borrar: se vuelven a elegir cuando llega el listado nuevo.
    private Set<Integer> aElegir = Set.of();
    private final JComboBox<Opcion<Integer>> filtroPelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> filtroSala = new JComboBox<>();
    private final FormularioFuncion formulario;
    private final Tabla<Funcion> tabla = new Tabla<>(
            Columna.<Funcion>de("Cuándo", f -> dia(f.inicio()) + "  " + hora(f.inicio())).ancho(140),
            Columna.<Funcion>de("Película", f -> f.pelicula().titulo()).ancho(200),
            Columna.<Funcion>de("Sala", f -> f.sala().nombre()),
            Columna.<Funcion>de("Formato", f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())).ancho(130),
            Columna.<Funcion>numero("Precio", f -> precio(f.precio())))
            .vacia("Ninguna función con esos filtros")
            .multiple();

    public PantallaFunciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiFunciones apiFunciones,
                             ApiSalas apiSalas, Navegacion navegacion) {
        this(apiCartelera, apiCatalogos, apiFunciones, apiSalas, navegacion, null, null);
    }

    // Los diálogos aparte: en headless no se pueden abrir, y los tests contestan y leen por el encargado.
    PantallaFunciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiFunciones apiFunciones,
                      ApiSalas apiSalas, Navegacion navegacion, Predicate<Pregunta> confirmar,
                      Consumer<String> rechazo) {
        super("Funciones", "Doble clic en una función abre su borderó e informe. "
                + "Con Shift o Cmd/Ctrl‑clic se eligen varias para borrarlas juntas.");
        this.apiFunciones = apiFunciones;
        this.navegacion = navegacion;
        this.borrar = new BorrarFunciones(apiFunciones);
        this.confirmar = confirmar != null ? confirmar
                : p -> Mensajes.confirmar(this, p.texto(), p.si(), p.no());
        this.rechazo = rechazo != null ? rechazo : errores -> Mensajes.error(this, errores);
        this.formulario = new FormularioFuncion(apiFunciones, this::recargar);

        filtroPelicula.setPrototypeDisplayValue(new Opcion<>(0, "Película de título"));
        JButton buscar = new JButton("Buscar");
        buscar.addActionListener(e -> buscar());
        filtros.combo("Película", "peliculaId", filtroPelicula)
                .combo("Sala", "salaId", filtroSala)
                .fecha("Desde", "desde", Fechas.selector(null))
                .fecha("Hasta", "hasta", Fechas.selector(null))
                .boton(buscar)
                .limpiar(this::recargar);

        add(Componentes.listado(filtros, filtros.conteo(), accionesDeFila(), tabla.conScroll()), BorderLayout.CENTER);
        add(formulario, BorderLayout.EAST);

        tabla.alDobleClic(this::abrirInformes);
        cargar(() -> new Catalogos(apiCartelera.obtenerPeliculas(null), apiSalas.obtenerSalas(),
                apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), catalogos -> {
            filtros.enSilencio(() -> {
                Campos.llenarConTodas(filtroPelicula, "Todas", Opciones.peliculas(catalogos.peliculas()));
                Campos.llenarConTodas(filtroSala, "Todas", Opciones.salas(catalogos.salas()));
            });
            formulario.llenar(catalogos.peliculas(), catalogos.salas(), catalogos.idiomas(),
                    catalogos.proyecciones());
            recargar();
        });
    }

    @Override
    protected List<Funcion> obtener(Map<String, String> filtros) {
        return apiFunciones.obtenerFunciones(filtros);
    }

    @Override
    protected Tabla<Funcion> tabla() {
        return tabla;
    }

    @Override
    protected String sinFiltrar(List<Funcion> todas) {
        return todas.size() + " programadas";
    }

    @Override
    protected void alMostrar(List<Funcion> visibles) {
        if (aElegir.isEmpty()) return;
        Set<Integer> ids = aElegir;
        aElegir = Set.of();
        tabla.seleccionar(f -> ids.contains(f.id()));
    }

    private JPanel accionesDeFila() {
        // Lo filtrado y no todo: «borrar todas» es filtrar, elegir todas y Borrar, sin un botón que borre a ciegas.
        JButton todas = Componentes.deTexto(new JButton("Seleccionar todas"));
        todas.setToolTipText("Las de la lista, como Cmd/Ctrl+A");
        todas.addActionListener(e -> tabla.seleccionarTodas());
        JButton informes = new JButton("Borderó e informe");
        informes.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirInformes));
        // La vista no trae si tiene reservas: el 409 del backend (R12) se muestra tal cual.
        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(formulario::editar));
        JButton borrarlas = Componentes.peligro(new JButton("Borrar"));
        borrarlas.addActionListener(e -> borrarElegidas());
        Runnable refrescar = () -> {
            boolean una = tabla.seleccionada().isPresent();
            editar.setEnabled(una);
            informes.setEnabled(una);
            borrarlas.setEnabled(!tabla.seleccionadas().isEmpty());
        };
        tabla.tabla().getSelectionModel().addListSelectionListener(e -> refrescar.run());
        refrescar.run();
        return Componentes.aLaDerecha(todas, editar, informes, borrarlas);
    }

    // Un pedido para todas: de a una eran 80 viajes a Render. Las rechazadas quedan elegidas y con su porqué.
    private void borrarElegidas() {
        List<Funcion> elegidas = tabla.seleccionadas();
        if (elegidas.isEmpty() || !confirmar.test(borrar.pregunta(elegidas))) return;
        cargar(() -> borrar.ejecutar(elegidas), borrado -> {
            BorrarFunciones.resumen(borrado).ifPresent(this::avisar);
            aElegir = borrado.rechazadas().stream().map(FuncionRechazada::id).collect(Collectors.toSet());
            recargar();
            BorrarFunciones.errores(borrado).ifPresent(rechazo);
        });
    }

    private void abrirInformes(Funcion funcion) {
        navegacion.abrirInforme(funcion.id());
    }
}
