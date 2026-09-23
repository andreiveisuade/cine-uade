package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Clasificacion;
import ar.uade.cine.swing.api.dto.PedidoPelicula;
import ar.uade.cine.swing.api.dto.Pelicula;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
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

    private record Resultado(List<Pelicula> todas, List<Pelicula> visibles) {
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
    private final JTextField duracionMinutos = new JTextField();
    private final JTextField anio = new JTextField();
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
    private Pelicula editando;
    private boolean llenando;
    // El texto espera a que se deje de tipear: sin eso, "Matrix" son seis pedidos.
    private final Timer espera = new Timer(250, e -> buscar());

    PantallaPeliculas(ApiHttp api, Navegacion navegacion) {
        super(api, "Películas", "Una película llega a la cartelera cuando tiene funciones por delante; "
                + "despublicarla la baja aunque las tenga.");
        espera.setRepeats(false);

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
            buscar();
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

        buscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                espera.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                espera.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                espera.restart();
            }
        });
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
        publicar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> accion(
                () -> api.actualizarPelicula(p.id(), PedidoPelicula.soloPublicacion(!p.enCartelera())),
                p.enCartelera() ? "Despublicada" : "Publicada", this::buscar)));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (!confirmar("¿Borrar " + p.titulo() + "?")) return;
            accion(() -> {
                api.eliminarPelicula(p.id());
                return null;
            }, "Película borrada", this::buscar);
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
                .campo("Título", titulo)
                .campo("Duración (min)", duracionMinutos)
                .campo("Año", anio)
                .campo("Clasificación", clasificacion)
                .campo("Dirección", director)
                .campo("Idioma original", idiomaOriginal)
                .campo("Sinopsis", new JScrollPane(sinopsis))
                .campo("Poster (URL)", posterUrl)
                .ancho(new JLabel("Géneros (al menos uno)"))
                .ancho(panelGeneros)
                .ancho(publicada)
                .ancho(botones)
                .cerrar();
        JScrollPane scroll = new JScrollPane(Componentes.conBorde(formulario));
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(360, 0));
        return scroll;
    }

    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("q", buscar.getText());
        filtros.put("genero", Campos.elegido(filtroGenero));
        filtros.put("publicada", Campos.elegido(filtroEstado));
        return filtros;
    }

    private void buscar() {
        if (llenando) return;
        Map<String, String> filtros = filtros();
        cargar(() -> new Resultado(api.obtenerPeliculas(null), api.obtenerPeliculas(filtros)), r -> {
            tabla.mostrar(r.visibles());
            long publicadas = r.todas().stream().filter(Pelicula::enCartelera).count();
            conteo.setText(r.todas().size() + " cargadas · " + publicadas + " publicadas"
                    + (r.visibles().size() == r.todas().size() ? "" : " · mostrando " + r.visibles().size()));
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
    }

    private void guardar() {
        List<String> elegidos = generos.stream().filter(JCheckBox::isSelected)
                .map(c -> (String) c.getClientProperty("genero")).toList();
        // Los textos viajan aunque estén vacíos, para poder borrar un director al editar; un título vacío lo
        // rechaza el backend con su mensaje. Un número que no es número viaja null y el mensaje también es de allá.
        PedidoPelicula pedido = new PedidoPelicula(titulo.getText().trim(), Campos.entero(duracionMinutos), elegidos,
                Campos.elegido(clasificacion), director.getText().trim(), sinopsis.getText().trim(),
                Campos.entero(anio), idiomaOriginal.getText().trim(), posterUrl.getText().trim(),
                publicada.isSelected());
        Pelicula actual = editando;
        if (actual == null) {
            accion(() -> api.crearPelicula(pedido), "Película agregada", () -> {
                limpiarFormulario();
                buscar();
            });
        } else {
            accion(() -> api.actualizarPelicula(actual.id(), pedido), "Cambios guardados", () -> {
                limpiarFormulario();
                buscar();
            });
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
