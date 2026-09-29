package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.Clasificacion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;

/** Catálogo completo, incluso lo que no está en cartelera; alta, edición, publicación y baja. */
final class PantallaPeliculas extends Pantalla {

    private record Catalogos(List<String> generos, List<Clasificacion> clasificaciones) {
    }

    private final JTextField buscar = new JTextField(16);
    private final JComboBox<Opcion<String>> filtroGenero = new JComboBox<>();
    private final JComboBox<Opcion<String>> filtroEstado = new JComboBox<>();
    private final JLabel conteo = new JLabel(" ");
    private final Tabla<Pelicula> tabla = new Tabla<>(
            Columna.<Pelicula>de("Título", p -> p.titulo() + (p.anio() > 0 ? " (" + p.anio() + ")" : "")).ancho(260),
            Columna.<Pelicula>de("Dirección", p -> p.director() == null || p.director().isEmpty() ? "—"
                    : p.director()),
            Columna.<Pelicula>de("Duración", p -> duracion(p.duracionMinutos())),
            Columna.<Pelicula>de("Edad", p -> etiqueta(p.clasificacion())),
            Columna.<Pelicula>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                    .collect(Collectors.joining(", "))).ancho(180),
            Columna.<Pelicula>de("Estado", p -> p.enCartelera() ? "Publicada" : "Despublicada"));

    private final JLabel tituloFormulario = Componentes.subtitulo("Nueva película");
    private final JTextField titulo = new JTextField();
    private final JTextField duracionMinutos = Campos.soloEntero(new JTextField());
    private final JTextField anio = Campos.soloEntero(new JTextField());
    private final JComboBox<Opcion<String>> clasificacion = new JComboBox<>();
    private final JTextField director = new JTextField();
    private final JTextField idiomaOriginal = new JTextField();
    private final JTextArea sinopsis = new JTextArea(4, 20);
    private final JTextField posterUrl = new JTextField();
    private final JPanel panelGeneros = new JPanel(new GridLayout(0, 2));
    private final List<JCheckBox> generos = new ArrayList<>();
    private final JCheckBox publicada = new JCheckBox("Publicada", true);
    private final JButton guardar = new JButton("Agregar");
    private final JButton cancelar = new JButton("Cancelar");
    private final JLabel error = Componentes.texto(" ");
    // El catálogo sin filtro, para el "N cargadas · M publicadas": se pide al entrar y después de cada cambio, no en
    // cada tecla del buscador.
    private List<Pelicula> todas = List.of();
    private Pelicula editando;
    private boolean llenando;
    private final Timer espera = Campos.alDejarDeTipear(buscar, this::buscar);

    PantallaPeliculas(ApiHttp api) {
        super(api, "Películas", "Una película llega a la cartelera cuando tiene funciones por delante; "
                + "despublicarla la baja aunque las tenga.");

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(barraFiltros(), BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(accionesDeFila(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(formulario(), BorderLayout.EAST);
        tabla.alDobleClic(this::editar);

        cargar(() -> new Catalogos(api.obtenerGeneros(), api.obtenerClasificaciones()), catalogos -> {
            llenando = true;
            filtroGenero.addItem(new Opcion<>(null, "Todos"));
            Opcion.de(catalogos.generos(), g -> etiqueta(g)).forEach(filtroGenero::addItem);
            for (Clasificacion c : catalogos.clasificaciones()) {
                clasificacion.addItem(new Opcion<>(c.nombre(), etiqueta(c.nombre())
                        + (c.edadMinima() > 0 ? " — desde " + c.edadMinima() + " años" : " — todo público")));
            }
            for (String g : catalogos.generos()) {
                JCheckBox caja = new JCheckBox(etiqueta(g));
                caja.putClientProperty("genero", g);
                generos.add(caja);
                panelGeneros.add(caja);
            }
            panelGeneros.revalidate();
            llenando = false;
            recargar();
        });
    }

    private JPanel barraFiltros() {
        filtroEstado.addItem(new Opcion<>(null, "Todas"));
        filtroEstado.addItem(new Opcion<>("true", "Publicadas"));
        filtroEstado.addItem(new Opcion<>("false", "Despublicadas"));
        JButton limpiar = new JButton("Limpiar");
        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Buscar"));
        barra.add(buscar);
        barra.add(new JLabel("Género"));
        barra.add(filtroGenero);
        barra.add(new JLabel("Estado"));
        barra.add(filtroEstado);
        barra.add(limpiar);
        barra.add(conteo);

        filtroGenero.addActionListener(e -> buscar());
        filtroEstado.addActionListener(e -> buscar());
        limpiar.addActionListener(e -> {
            llenando = true;
            buscar.setText("");
            filtroGenero.setSelectedIndex(0);
            filtroEstado.setSelectedIndex(0);
            llenando = false;
            espera.stop();
            buscar();
        });
        return barra;
    }

    private JPanel accionesDeFila() {
        JButton editar = new JButton("Editar");
        JButton publicar = new JButton("Publicar / despublicar");
        JButton borrar = new JButton("Borrar");
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(this::editar));
        publicar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            // Despublicar la baja de la cartelera del cliente aunque tenga funciones: se pregunta. Publicar no.
            if (p.enCartelera() && !confirmar("¿Despublicar " + p.titulo() + "? Deja de verse en la cartelera "
                    + "aunque tenga funciones.", "Sí, despublicar")) return;
            accion(() -> api.actualizarPelicula(p.id(), PedidoPelicula.soloPublicacion(!p.enCartelera())),
                    p.enCartelera() ? "Despublicada" : "Publicada", this::recargar);
        }));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (!confirmar("¿Borrar " + p.titulo() + "?", "Sí, borrar")) return;
            accion(() -> {
                api.eliminarPelicula(p.id());
                return null;
            }, "Película borrada", this::recargar);
        }));
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(editar);
        acciones.add(publicar);
        acciones.add(borrar);
        return acciones;
    }

    private JScrollPane formulario() {
        sinopsis.setLineWrap(true);
        sinopsis.setWrapStyleWord(true);
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> limpiarFormulario());
        cancelar.setVisible(false);
        JPanel botones = new JPanel(new GridLayout(1, 2, 6, 0));
        botones.add(guardar);
        botones.add(cancelar);

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(tituloFormulario)
                .obligatorio("Título", titulo)
                .obligatorio("Duración (min)", duracionMinutos)
                .campo("Año", anio)
                .obligatorio("Clasificación", clasificacion)
                .campo("Dirección", director)
                .campo("Idioma original", idiomaOriginal)
                .campo("Sinopsis", new JScrollPane(sinopsis))
                .campo("Poster (URL)", posterUrl)
                .ancho(new JLabel("Géneros (al menos uno) *"))
                .ancho(panelGeneros)
                .ancho(publicada)
                .ancho(botones)
                .ancho(error)
                .cerrar();
        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("q", buscar.getText());
        filtros.put("genero", Campos.elegido(filtroGenero));
        filtros.put("publicada", Campos.elegido(filtroEstado));
        return filtros;
    }

    private void recargar() {
        cargar(() -> api.obtenerPeliculas(null), lista -> {
            todas = lista;
            buscar();
        });
    }

    private void buscar() {
        if (llenando) return;
        Map<String, String> filtros = filtros();
        cargar(() -> api.obtenerPeliculas(filtros), visibles -> {
            tabla.mostrar(visibles);
            long publicadas = todas.stream().filter(Pelicula::enCartelera).count();
            conteo.setText(todas.size() + " cargadas · " + publicadas + " publicadas"
                    + (visibles.size() == todas.size() ? "" : " · mostrando " + visibles.size()));
        });
    }

    private void editar(Pelicula pelicula) {
        editando = pelicula;
        tituloFormulario.setText("Editar " + pelicula.titulo());
        titulo.setText(pelicula.titulo());
        duracionMinutos.setText(String.valueOf(pelicula.duracionMinutos()));
        anio.setText(pelicula.anio() > 0 ? String.valueOf(pelicula.anio()) : "");
        Campos.elegir(clasificacion, pelicula.clasificacion());
        director.setText(texto(pelicula.director()));
        idiomaOriginal.setText(texto(pelicula.idiomaOriginal()));
        sinopsis.setText(texto(pelicula.sinopsis()));
        posterUrl.setText(texto(pelicula.posterUrl()));
        generos.forEach(c -> c.setSelected(pelicula.generos().contains((String) c.getClientProperty("genero"))));
        publicada.setSelected(pelicula.enCartelera());
        // Al principio y no al final: en el formulario angosto, un título largo se tiene que leer desde el comienzo.
        for (JTextComponent campo : List.of(titulo, sinopsis, posterUrl)) campo.setCaretPosition(0);
        guardar.setText("Guardar cambios");
        cancelar.setVisible(true);
    }

    private void limpiarFormulario() {
        editando = null;
        tituloFormulario.setText("Nueva película");
        for (JTextField campo : List.of(titulo, duracionMinutos, anio, director, idiomaOriginal, posterUrl)) {
            campo.setText("");
        }
        sinopsis.setText("");
        if (clasificacion.getItemCount() > 0) clasificacion.setSelectedIndex(0);
        generos.forEach(c -> c.setSelected(false));
        publicada.setSelected(true);
        guardar.setText("Agregar");
        cancelar.setVisible(false);
        error.setText(" ");
    }

    private void guardar() {
        List<String> elegidos = generos.stream().filter(JCheckBox::isSelected)
                .map(c -> (String) c.getClientProperty("genero")).toList();
        Validacion v = new Validacion(error);
        String tituloLeido = v.texto(titulo, "Título", true);
        Integer duracion = v.entero(duracionMinutos, "Duración", true);
        Integer anioLeido = v.entero(anio, "Año", false);
        String clasificacionElegida = v.elegido(clasificacion, "Clasificación");
        v.exigir(!elegidos.isEmpty(), panelGeneros, "Géneros");
        if (!v.ok()) return;
        // Los textos opcionales viajan aunque estén vacíos, para poder borrar un director al editar.
        PedidoPelicula pedido = new PedidoPelicula(tituloLeido, duracion, elegidos, clasificacionElegida,
                director.getText().trim(), sinopsis.getText().trim(), anioLeido, idiomaOriginal.getText().trim(),
                posterUrl.getText().trim(), publicada.isSelected());
        Pelicula actual = editando;
        guardar.setEnabled(false);
        Tarea.ejecutar(this, () -> actual == null ? api.crearPelicula(pedido)
                : api.actualizarPelicula(actual.id(), pedido), guardada -> {
            guardar.setEnabled(true);
            avisar(actual == null ? "Película agregada" : "Cambios guardados");
            limpiarFormulario();
            recargar();
        }, e -> {
            guardar.setEnabled(true);
            v.mostrarError(e);
        });
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
