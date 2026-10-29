package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ar.uade.cine.swing.comun.Formato.numero;

// El alta y la edición de una función suelta, al costado del listado; R3 y R8 no se anticipan: el backend las valida.
final class FormularioFuncion extends Seccion {

    private final ApiFunciones apiFunciones;
    private final Runnable alGuardar;
    private final JLabel titulo = Componentes.subtitulo("Programar función");
    private final JComboBox<Opcion<Integer>> pelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final CampoFecha dia = new CampoFecha(LocalDate.now());
    private final JSpinner hora = Fechas.hora(proximaHora());
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JTextField precioBase = Campos.soloDecimal(new JTextField());
    private final JButton guardar = Componentes.primario(new JButton("Programar"));
    private final JButton cancelar = new JButton("Cancelar");
    private final JLabel error = Componentes.texto(" ");
    private Funcion editando;

    FormularioFuncion(ApiFunciones apiFunciones, Runnable alGuardar) {
        super(new BorderLayout());
        this.apiFunciones = apiFunciones;
        this.alGuardar = alGuardar;
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> limpiar());
        cancelar.setVisible(false);
        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        add(Componentes.lateral(Componentes.conBorde(new Formulario()
                .ancho(titulo)
                .obligatorio("Película", pelicula)
                .obligatorio("Sala", sala)
                .obligatorio("Día", dia)
                .obligatorio("Hora", hora)
                .obligatorio("Idioma", idioma)
                .obligatorio("Proyección", proyeccion)
                .obligatorio("Precio base", precioBase)
                .ancho(Componentes.aLaDerecha(cancelar, guardar))
                .ancho(error)
                .cerrar())));
    }

    void llenar(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas, List<String> proyecciones) {
        Campos.llenar(pelicula, Opciones.peliculasConDuracion(peliculas));
        Campos.llenar(sala, Opciones.salasConTipo(salas));
        Campos.llenar(idioma, Opciones.etiquetadas(idiomas));
        Campos.llenar(proyeccion, Opciones.etiquetadas(proyecciones));
    }

    /** Precarga la función; guardar la edita en vez de programar otra. */
    void editar(Funcion f) {
        editando = f;
        LocalDateTime inicio = LocalDateTime.parse(f.inicio());
        titulo.setText("Editar función");
        Campos.elegir(pelicula, f.peliculaId());
        Campos.elegir(sala, f.salaId());
        dia.poner(inicio.toLocalDate());
        hora.setValue(Fechas.aDate(inicio));
        Campos.elegir(idioma, f.idioma());
        Campos.elegir(proyeccion, f.proyeccion());
        precioBase.setText(numero(f.precio()));
        guardar.setText("Guardar");
        cancelar.setVisible(true);
        error.setText(" ");
    }

    private void guardar() {
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
        Funcion actual = editando;
        Tarea.ejecutar(this, () -> actual == null ? apiFunciones.programarFuncion(pedido)
                : apiFunciones.editarFuncion(actual.id(), pedido), guardada -> {
            avisar(actual == null ? "Función programada" : "Cambios guardados");
            limpiar();
            alGuardar.run();
        }, v::mostrarError);
    }

    private void limpiar() {
        editando = null;
        titulo.setText("Programar función");
        if (pelicula.getItemCount() > 0) pelicula.setSelectedIndex(0);
        if (sala.getItemCount() > 0) sala.setSelectedIndex(0);
        dia.poner(LocalDate.now());
        hora.setValue(Fechas.aDate(LocalDate.now().atTime(proximaHora())));
        if (idioma.getItemCount() > 0) idioma.setSelectedIndex(0);
        if (proyeccion.getItemCount() > 0) proyeccion.setSelectedIndex(0);
        precioBase.setText("");
        guardar.setText("Programar");
        cancelar.setVisible(false);
        error.setText(" ");
    }

    private static LocalTime proximaHora() {
        return LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
    }
}
