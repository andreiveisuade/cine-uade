package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.Clasificacion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Alta y edición de una película, al costado del catálogo: el mismo formulario sirve para las dos.
final class FormularioPelicula extends Seccion {

    private final ApiCartelera apiCartelera;
    private final Runnable alGuardar;
    private final JLabel tituloFormulario = Componentes.subtitulo("Nueva película");
    private final JTextField titulo = new JTextField();
    private final JTextField duracionMinutos = Campos.soloEntero(new JTextField());
    private final JTextField anio = Campos.soloEntero(new JTextField());
    private final JComboBox<Opcion<String>> clasificacion = new JComboBox<>();
    private final JTextField director = new JTextField();
    private final JTextField idiomaOriginal = new JTextField();
    private final JTextArea sinopsis = new JTextArea(4, 20);
    private final JTextField posterUrl = new JTextField();
    private final CampoTrailer trailer = new CampoTrailer();
    private final JPanel panelGeneros = new JPanel(new GridLayout(0, 2));
    private final List<JCheckBox> generos = new ArrayList<>();
    private final JPanel panelFormatos = new JPanel(new GridLayout(0, 2));
    private final List<JCheckBox> formatos = new ArrayList<>();
    private final JCheckBox publicada = new JCheckBox("Publicada", true);
    private final JButton guardar = Componentes.primario(new JButton("Agregar"));
    private final JButton cancelar = new JButton("Cancelar");
    private final JLabel error = Componentes.texto(" ");
    private Pelicula editando;

    FormularioPelicula(ApiCartelera apiCartelera, Runnable alGuardar) {
        super(new BorderLayout());
        this.apiCartelera = apiCartelera;
        this.alGuardar = alGuardar;
        sinopsis.setLineWrap(true);
        sinopsis.setWrapStyleWord(true);
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> limpiar());
        cancelar.setVisible(false);

        add(Componentes.lateral(Componentes.conBorde(new Formulario()
                .ancho(tituloFormulario)
                .obligatorio("Título", titulo)
                .obligatorio("Duración (min)", duracionMinutos)
                .campo("Año", anio)
                .obligatorio("Clasificación", clasificacion)
                .campo("Dirección", director)
                .campo("Idioma original", idiomaOriginal)
                .campo("Sinopsis", new JScrollPane(sinopsis))
                .campo("Poster (URL)", posterUrl)
                .campo("Trailer (YouTube)", trailer)
                .ancho(new JLabel("Géneros (al menos uno) *"))
                .ancho(panelGeneros)
                .ancho(new JLabel("Formatos (al menos uno) *"))
                .ancho(panelFormatos)
                .ancho(publicada)
                .ancho(Componentes.aLaDerecha(cancelar, guardar))
                .ancho(error)
                .cerrar())));
    }

    void llenar(List<String> catalogoGeneros, List<Clasificacion> clasificaciones, List<String> proyecciones) {
        for (Clasificacion c : clasificaciones) {
            clasificacion.addItem(new Opcion<>(c.nombre(), etiqueta(c.nombre())
                    + (c.edadMinima() > 0 ? " — desde " + c.edadMinima() + " años" : " — todo público")));
        }
        for (String g : catalogoGeneros) {
            JCheckBox caja = new JCheckBox(etiqueta(g));
            caja.putClientProperty("genero", g);
            generos.add(caja);
            panelGeneros.add(caja);
        }
        for (String f : proyecciones) {
            JCheckBox caja = new JCheckBox(etiqueta(f));
            caja.putClientProperty("formato", f);
            formatos.add(caja);
            panelFormatos.add(caja);
        }
        marcarFormatoPorDefecto();
        panelGeneros.revalidate();
        panelFormatos.revalidate();
    }

    void editar(Pelicula pelicula) {
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
        trailer.poner(texto(pelicula.trailerYoutube()));
        generos.forEach(c -> c.setSelected(pelicula.generos().contains((String) c.getClientProperty("genero"))));
        formatos.forEach(c -> c.setSelected(pelicula.formatos().contains((String) c.getClientProperty("formato"))));
        publicada.setSelected(pelicula.enCartelera());
        // Al principio y no al final: en el formulario angosto, un título largo se tiene que leer desde el comienzo.
        for (JTextComponent campo : List.of(titulo, sinopsis, posterUrl)) campo.setCaretPosition(0);
        guardar.setText("Guardar cambios");
        cancelar.setVisible(true);
    }

    private void limpiar() {
        editando = null;
        tituloFormulario.setText("Nueva película");
        for (JTextField campo : List.of(titulo, duracionMinutos, anio, director, idiomaOriginal, posterUrl)) {
            campo.setText("");
        }
        trailer.poner("");
        sinopsis.setText("");
        if (clasificacion.getItemCount() > 0) clasificacion.setSelectedIndex(0);
        generos.forEach(c -> c.setSelected(false));
        marcarFormatoPorDefecto();
        publicada.setSelected(true);
        guardar.setText("Agregar");
        cancelar.setVisible(false);
        error.setText(" ");
    }

    private void guardar() {
        List<String> elegidos = generos.stream().filter(JCheckBox::isSelected)
                .map(c -> (String) c.getClientProperty("genero")).toList();
        List<String> formatosElegidos = formatos.stream().filter(JCheckBox::isSelected)
                .map(c -> (String) c.getClientProperty("formato")).toList();
        Validacion v = new Validacion(error);
        String tituloLeido = v.texto(titulo, "Título", true);
        Integer duracion = v.entero(duracionMinutos, "Duración", true);
        Integer anioLeido = v.entero(anio, "Año", false);
        String clasificacionElegida = v.elegido(clasificacion, "Clasificación");
        v.exigir(!elegidos.isEmpty(), panelGeneros, "Géneros");
        v.exigir(!formatosElegidos.isEmpty(), panelFormatos, "Formatos");
        v.alMencionar("formato", panelFormatos);
        if (!v.ok()) return;
        // Los textos opcionales viajan aunque estén vacíos, para poder borrar un director al editar.
        PedidoPelicula pedido = new PedidoPelicula(tituloLeido, duracion, elegidos, clasificacionElegida,
                director.getText().trim(), sinopsis.getText().trim(), anioLeido, idiomaOriginal.getText().trim(),
                posterUrl.getText().trim(), publicada.isSelected(), trailer.texto(), formatosElegidos);
        Pelicula actual = editando;
        guardar.setEnabled(false);
        Tarea.ejecutar(this, () -> actual == null ? apiCartelera.crearPelicula(pedido)
                : apiCartelera.actualizarPelicula(actual.id(), pedido), guardada -> {
            guardar.setEnabled(true);
            avisar(actual == null ? "Película agregada" : "Cambios guardados");
            limpiar();
            alGuardar.run();
        }, e -> {
            guardar.setEnabled(true);
            v.mostrarError(e);
        });
    }

    // El catálogo llega en orden y el primero es 2D: el mismo formato que el backend le da al alta sin formatos.
    private void marcarFormatoPorDefecto() {
        formatos.forEach(c -> c.setSelected(formatos.indexOf(c) == 0));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
