package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.Clasificacion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Poster;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.PantallaListado;
import ar.uade.cine.swing.pantallas.acciones.BotonesDeFila;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;

// El catálogo completo, aun lo que no está en cartelera: publicar y borrar acá, alta y edición al costado.
public final class PantallaPeliculas extends PantallaListado<Pelicula> {

    private record Catalogos(List<String> generos, List<Clasificacion> clasificaciones) {
    }

    private static final int ALTO_POSTER = 40;

    private final ApiCartelera apiCartelera;
    private final ApiImagenes apiImagenes;
    private final Map<Integer, Icon> posters = new HashMap<>();
    private final JComboBox<Opcion<String>> filtroGenero = new JComboBox<>();
    private final FormularioPelicula formulario;
    private final Tabla<Pelicula> tabla = new Tabla<>(
            Columna.<Pelicula>de("", p -> posters.get(p.id())).ancho(44),
            Columna.<Pelicula>de("Título", p -> p.titulo() + (p.anio() > 0 ? " (" + p.anio() + ")" : "")).ancho(220),
            Columna.<Pelicula>de("Duración", p -> duracion(p.duracionMinutos())),
            Columna.<Pelicula>de("Edad", p -> etiqueta(p.clasificacion())),
            Columna.<Pelicula>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                    .collect(Collectors.joining(", "))).ancho(180),
            Columna.<Pelicula>de("Estado", p -> p.enCartelera() ? "Publicada" : "Despublicada").ancho(110)
                    .comoEstado(p -> p.enCartelera() ? Colores.exito() : Colores.secundario()))
            .vacia("Ninguna película con esos filtros");

    public PantallaPeliculas(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos, ApiImagenes apiImagenes) {
        super("Películas", "Una película llega a la cartelera cuando tiene funciones por delante; "
                + "despublicarla la baja aunque las tenga.");
        this.apiCartelera = apiCartelera;
        this.apiImagenes = apiImagenes;
        tabla.tabla().setRowHeight(ALTO_POSTER + Componentes.PEGADO * 2);
        this.formulario = new FormularioPelicula(apiCartelera, this::recargar);

        JComboBox<Opcion<String>> filtroEstado = new JComboBox<>();
        filtroEstado.addItem(new Opcion<>(null, "Todas"));
        filtroEstado.addItem(new Opcion<>("true", "Publicadas"));
        filtroEstado.addItem(new Opcion<>("false", "Despublicadas"));
        filtros.texto("Buscar", "q", new JTextField(12))
                .combo("Género", "genero", filtroGenero)
                .combo("Estado", "publicada", filtroEstado)
                .limpiar(this::buscar);

        add(Componentes.listado(filtros, filtros.conteo(), accionesDeFila(), tabla.conScroll()), BorderLayout.CENTER);
        add(formulario, BorderLayout.EAST);
        tabla.alDobleClic(formulario::editar);

        cargar(() -> new Catalogos(apiCatalogos.obtenerGeneros(), apiCatalogos.obtenerClasificaciones()), c -> {
            filtros.enSilencio(() -> Campos.llenarConTodas(filtroGenero, "Todos", Opciones.etiquetadas(c.generos())));
            formulario.llenar(c.generos(), c.clasificaciones());
            recargar();
        });
    }

    @Override
    protected List<Pelicula> obtener(Map<String, String> filtros) {
        return apiCartelera.obtenerPeliculas(filtros);
    }

    // El póster reconoce la película de un vistazo; llega de a uno, sin hacer esperar al listado.
    @Override
    protected void alMostrar(List<Pelicula> visibles) {
        for (Pelicula p : visibles) {
            Poster.miniatura(this, apiImagenes, p.posterUrl(), ALTO_POSTER, icono -> {
                posters.put(p.id(), icono);
                tabla.tabla().repaint();
            });
        }
    }

    @Override
    protected Tabla<Pelicula> tabla() {
        return tabla;
    }

    @Override
    protected String sinFiltrar(List<Pelicula> todas) {
        long publicadas = todas.stream().filter(Pelicula::enCartelera).count();
        return todas.size() + " cargadas · " + publicadas + " publicadas";
    }

    // El total del catálogo se ve siempre, filtre lo que filtre: es lo primero que se pregunta quien lo arma.
    @Override
    protected String conteo(List<Pelicula> todas, List<Pelicula> visibles) {
        return sinFiltrar(todas) + (visibles.size() == todas.size() ? "" : " · mostrando " + visibles.size());
    }

    private JPanel accionesDeFila() {
        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(formulario::editar));
        JPanel acciones = Componentes.aLaDerecha(editar);
        // Command: cada operación es un objeto y BotonesDeFila la dispara (ver AccionDeFila).
        new BotonesDeFila<>(this, tabla, this::recargar, new PublicarPelicula(apiCartelera),
                new BorrarPelicula(apiCartelera)).agregarA(acciones);
        return acciones;
    }
}
