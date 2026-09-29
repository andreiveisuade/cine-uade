package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.PantallaListado;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Listado con filtros de las funciones, con el alta al costado; doble clic abre su borderó e informe.
/**
 * No anticipa R3 ni R8 antes de mandar: el backend las valida igual y su mensaje llega tal cual, así la regla vive en
 * un solo lugar.
 */
public final class PantallaFunciones extends PantallaListado<Funcion> {

    private record Catalogos(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas,
                             List<String> proyecciones) {
    }

    private final ApiFunciones apiFunciones;
    private final Navegacion navegacion;
    private final JComboBox<Opcion<Integer>> filtroPelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> filtroSala = new JComboBox<>();
    private final FormularioFuncion formulario;
    private final Tabla<Funcion> tabla = new Tabla<>(
            Columna.<Funcion>de("Cuándo", f -> dia(f.inicio()) + "  " + hora(f.inicio())).ancho(140),
            Columna.<Funcion>de("Película", f -> f.pelicula().titulo()).ancho(260),
            Columna.<Funcion>de("Sala", f -> f.sala().nombre()),
            Columna.<Funcion>de("Formato", f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())),
            Columna.<Funcion>numero("Precio", f -> precio(f.precio())));

    public PantallaFunciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiFunciones apiFunciones,
                             ApiSalas apiSalas, Navegacion navegacion) {
        super("Funciones", "Es la lista más larga del panel: una semana de seis salas pasa de cien funciones. "
                + "Doble clic en una función abre su borderó e informe.");
        this.apiFunciones = apiFunciones;
        this.navegacion = navegacion;
        this.formulario = new FormularioFuncion(apiFunciones, this::recargar);

        filtroPelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        JButton buscar = new JButton("Buscar");
        buscar.addActionListener(e -> buscar());
        filtros.combo("Película", "peliculaId", filtroPelicula)
                .combo("Sala", "salaId", filtroSala)
                .fecha("Desde", "desde", Fechas.selector(null))
                .fecha("Hasta", "hasta", Fechas.selector(null))
                .boton(buscar)
                .limpiar(this::recargar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(accionesDeFila(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
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
