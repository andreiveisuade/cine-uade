package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.Clasificacion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.PantallaListado;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;

// El catálogo completo, aun lo que no está en cartelera: publicar y borrar acá, alta y edición al costado.
public final class PantallaPeliculas extends PantallaListado<Pelicula> {

    private record Catalogos(List<String> generos, List<Clasificacion> clasificaciones) {
    }

    private final ApiCartelera apiCartelera;
    private final JComboBox<Opcion<String>> filtroGenero = new JComboBox<>();
    private final FormularioPelicula formulario;
    private final Tabla<Pelicula> tabla = new Tabla<>(
            Columna.<Pelicula>de("Título", p -> p.titulo() + (p.anio() > 0 ? " (" + p.anio() + ")" : "")).ancho(260),
            Columna.<Pelicula>de("Dirección", p -> p.director() == null || p.director().isEmpty() ? "—"
                    : p.director()),
            Columna.<Pelicula>de("Duración", p -> duracion(p.duracionMinutos())),
            Columna.<Pelicula>de("Edad", p -> etiqueta(p.clasificacion())),
            Columna.<Pelicula>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                    .collect(Collectors.joining(", "))).ancho(180),
            Columna.<Pelicula>de("Estado", p -> p.enCartelera() ? "Publicada" : "Despublicada"));

    public PantallaPeliculas(ApiCartelera apiCartelera, ApiCatalogos apiCatalogos) {
        super("Películas", "Una película llega a la cartelera cuando tiene funciones por delante; "
                + "despublicarla la baja aunque las tenga.");
        this.apiCartelera = apiCartelera;
        this.formulario = new FormularioPelicula(apiCartelera, this::recargar);

        JComboBox<Opcion<String>> filtroEstado = new JComboBox<>();
        filtroEstado.addItem(new Opcion<>(null, "Todas"));
        filtroEstado.addItem(new Opcion<>("true", "Publicadas"));
        filtroEstado.addItem(new Opcion<>("false", "Despublicadas"));
        filtros.texto("Buscar", "q", new JTextField(16))
                .combo("Género", "genero", filtroGenero)
                .combo("Estado", "publicada", filtroEstado)
                .limpiar(this::buscar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(accionesDeFila(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
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
        JButton publicar = new JButton("Publicar / despublicar");
        JButton borrar = new JButton("Borrar");
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(formulario::editar));
        publicar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            // Despublicar la baja de la cartelera del cliente aunque tenga funciones: se pregunta. Publicar no.
            if (p.enCartelera() && !confirmar("¿Despublicar " + p.titulo() + "? Deja de verse en la cartelera "
                    + "aunque tenga funciones.", "Sí, despublicar")) return;
            accion(() -> apiCartelera.actualizarPelicula(p.id(), PedidoPelicula.soloPublicacion(!p.enCartelera())),
                    p.enCartelera() ? "Despublicada" : "Publicada", this::recargar);
        }));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (!confirmar("¿Borrar " + p.titulo() + "?", "Sí, borrar")) return;
            accion(() -> apiCartelera.eliminarPelicula(p.id()), "Película borrada", this::recargar);
        }));
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(editar);
        acciones.add(publicar);
        acciones.add(borrar);
        return acciones;
    }
}
