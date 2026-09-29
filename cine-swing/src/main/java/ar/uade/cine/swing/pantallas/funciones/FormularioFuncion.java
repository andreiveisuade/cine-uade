package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// El alta de una función suelta, al costado del listado; R3 y R8 no se anticipan: el backend las valida.
final class FormularioFuncion extends Seccion {

    private final ApiFunciones apiFunciones;
    private final Runnable alProgramar;
    private final JComboBox<Opcion<Integer>> pelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JDateChooser dia = Fechas.selector(LocalDate.now());
    private final JSpinner hora = Fechas.hora(LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1));
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JTextField precioBase = Campos.soloDecimal(new JTextField());
    private final JLabel error = Componentes.texto(" ");

    FormularioFuncion(ApiFunciones apiFunciones, Runnable alProgramar) {
        super(new BorderLayout());
        this.apiFunciones = apiFunciones;
        this.alProgramar = alProgramar;
        JButton programar = new JButton("Programar");
        programar.addActionListener(e -> programar());
        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        add(Componentes.lateral(Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Programar función"))
                .obligatorio("Película", pelicula)
                .obligatorio("Sala", sala)
                .obligatorio("Día", dia)
                .obligatorio("Hora", hora)
                .obligatorio("Idioma", idioma)
                .obligatorio("Proyección", proyeccion)
                .obligatorio("Precio base", precioBase)
                .ancho(programar)
                .ancho(error)
                .cerrar())));
    }

    void llenar(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas, List<String> proyecciones) {
        Campos.llenar(pelicula, Opciones.peliculasConDuracion(peliculas));
        Campos.llenar(sala, Opciones.salasConTipo(salas));
        Campos.llenar(idioma, Opciones.etiquetadas(idiomas));
        Campos.llenar(proyeccion, Opciones.etiquetadas(proyecciones));
    }

    private void programar() {
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
        Tarea.ejecutar(this, () -> apiFunciones.programarFuncion(pedido), creada -> {
            avisar("Función programada");
            alProgramar.run();
        }, v::mostrarError);
    }
}
