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
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.PantallaListado;
import ar.uade.cine.swing.pantallas.acciones.BotonesDeFila;

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
// Sin anticipar R3 ni R8: el backend las valida y su mensaje llega tal cual.
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
            Columna.<Funcion>numero("Precio", f -> precio(f.precio())))
            .vacia("Ninguna función con esos filtros");

    public PantallaFunciones(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiFunciones apiFunciones,
                             ApiSalas apiSalas, Navegacion navegacion) {
        super("Funciones", "Doble clic en una función abre su borderó e informe.");
        this.apiFunciones = apiFunciones;
        this.navegacion = navegacion;
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

    private JPanel accionesDeFila() {
        JButton informes = new JButton("Borderó e informe");
        informes.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirInformes));
        JPanel acciones = Componentes.aLaDerecha(informes);
        // Command: ver AccionDeFila.
        new BotonesDeFila<>(this, tabla, this::recargar, new BorrarFuncion(apiFunciones)).agregarA(acciones);
        return acciones;
    }

    private void abrirInformes(Funcion funcion) {
        navegacion.abrirInforme(funcion.id());
    }
}
