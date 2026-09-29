package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.programaciones.FuncionPlanificada;
import ar.uade.cine.swing.api.dto.programaciones.PedidoProgramacion;
import ar.uade.cine.swing.api.dto.programaciones.Plan;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static ar.uade.cine.swing.comun.Formato.fechaHora;

// Una grilla nueva: se previsualiza y solo entonces se confirma, con el informe de las fechas que chocan.
/** Tocar cualquier campo invalida la previsualización: nunca se confirma algo distinto de lo que se vio. */
final class FormularioProgramacion extends Seccion {

    private final ApiProgramaciones apiProgramaciones;
    private final Runnable alCrear;
    private final JComboBox<Opcion<Integer>> pelicula = new JComboBox<>();
    private final JComboBox<Opcion<Integer>> sala = new JComboBox<>();
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JDateChooser hasta = Fechas.selector(null);
    private final JSpinner horaInicio = Fechas.hora(LocalTime.of(20, 30));
    private final SelectorDias dias = new SelectorDias();
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JTextField precioBase = Campos.soloDecimal(new JTextField("5000"));
    private final JLabel error = Componentes.texto(" ");
    private final JButton confirmar = new JButton("Confirmar");
    private final JTextArea informe = Componentes.areaDeLectura(6, 30);
    // El plan previsualizado vale solo para los datos con que se pidió.
    private Plan previsualizado;

    FormularioProgramacion(ApiProgramaciones apiProgramaciones, Runnable alCrear) {
        super(new BorderLayout());
        this.apiProgramaciones = apiProgramaciones;
        this.alCrear = alCrear;
        JButton previsualizar = new JButton("Previsualizar");
        previsualizar.addActionListener(e -> previsualizar());
        confirmar.addActionListener(e -> confirmar());
        confirmar.setEnabled(false);
        JScrollPane scrollInforme = new JScrollPane(informe);
        scrollInforme.setPreferredSize(new Dimension(200, 180));

        Formulario formulario = new Formulario()
                .ancho(Componentes.subtitulo("Nueva grilla"))
                .obligatorio("Película", pelicula)
                .obligatorio("Sala", sala)
                .obligatorio("Desde", desde)
                .campo("Hasta", hasta)
                .ancho(Componentes.nota("Hasta vacío = sin fin."))
                .obligatorio("Hora", horaInicio)
                .ancho(new JLabel("Días (ninguno = todos)"))
                .ancho(dias)
                .obligatorio("Idioma", idioma)
                .obligatorio("Proyección", proyeccion)
                .obligatorio("Precio base", precioBase)
                .ancho(Componentes.botones(previsualizar, confirmar))
                .ancho(error)
                .ancho(scrollInforme)
                .ancho(Componentes.nota("Al confirmar, el servidor <b>vuelve a revisar</b> cada fecha: entre que "
                        + "mirás el informe y confirmás, otro puede haber programado algo en esa sala."))
                .cerrar();
        add(Componentes.lateral(Componentes.conBorde(formulario)));

        pelicula.setPrototypeDisplayValue(new Opcion<>(0, "Una película de título largo"));
        Campos.alCambiar(this::invalidar, pelicula, sala, idioma, proyeccion, desde, hasta, horaInicio, dias,
                precioBase);
    }

    void llenar(List<Pelicula> peliculas, List<Sala> salas, List<String> idiomas, List<String> proyecciones) {
        Campos.llenar(pelicula, Opciones.peliculasConDuracion(peliculas));
        Campos.llenar(sala, Opciones.salasConTipo(salas));
        Campos.llenar(idioma, Opciones.etiquetadas(idiomas));
        Campos.llenar(proyeccion, Opciones.etiquetadas(proyecciones));
    }

    private void invalidar() {
        if (previsualizado == null) return;
        previsualizado = null;
        confirmar.setEnabled(false);
        informe.setText("");
    }

    /** El pedido leído del formulario, o null si falta algo o hay algo mal tipeado: entonces no se manda. */
    private PedidoProgramacion pedido(Validacion v) {
        Integer peliculaId = v.elegido(pelicula, "Película");
        Integer salaId = v.elegido(sala, "Sala");
        String inicio = v.fecha(desde, "Desde", true);
        // `hasta` vacío viaja null: es una grilla abierta, no una fecha que falta.
        String fin = v.fecha(hasta, "Hasta", false);
        String idiomaElegido = v.elegido(idioma, "Idioma");
        String proyeccionElegida = v.elegido(proyeccion, "Proyección");
        Double precio = v.decimal(precioBase, "Precio base", true);
        v.alMencionar("rango", desde);
        if (!v.ok()) return null;
        return new PedidoProgramacion(peliculaId, salaId, inicio, fin, Fechas.leerHora(horaInicio).toString(),
                dias.elegidos(), idiomaElegido, proyeccionElegida, precio);
    }

    private void previsualizar() {
        Validacion v = new Validacion(error);
        PedidoProgramacion pedido = pedido(v);
        if (pedido == null) return;
        Tarea.ejecutar(this, () -> apiProgramaciones.previsualizarProgramacion(pedido), plan -> {
            informe.setText(textoDelPlan(plan, false));
            informe.setCaretPosition(0);
            previsualizado = plan;
            confirmar.setEnabled(plan.generadas() > 0);
        }, e -> {
            invalidar();
            v.mostrarError(e);
        });
    }

    private void confirmar() {
        Validacion v = new Validacion(error);
        PedidoProgramacion pedido = pedido(v);
        if (pedido == null) return;
        confirmar.setEnabled(false);
        Tarea.ejecutar(this, () -> apiProgramaciones.crearProgramacion(pedido), plan -> {
            // Se repinta con lo que devolvió el servidor, que revalidó cada fecha al aplicar.
            informe.setText(textoDelPlan(plan, true));
            informe.setCaretPosition(0);
            previsualizado = null;
            avisar("Grilla creada: " + plan.generadas() + " funciones"
                    + (plan.salteadas() > 0 ? ", " + plan.salteadas() + " salteadas" : ""));
            alCrear.run();
        }, e -> {
            confirmar.setEnabled(previsualizado != null);
            v.mostrarError(e);
        });
    }

    private static String textoDelPlan(Plan plan, boolean aplicado) {
        StringBuilder texto = new StringBuilder();
        texto.append(aplicado ? "Se generaron " : "Se van a generar ").append(plan.generadas()).append(" funciones");
        if (plan.salteadas() > 0) {
            texto.append(", ").append(plan.salteadas()).append(aplicado ? " se saltearon" : " se saltean");
        }
        texto.append("\n\n");
        for (FuncionPlanificada f : plan.funciones()) {
            texto.append(fechaHora(f.inicio()));
            if (f.choca()) texto.append("  ✗ ").append(f.motivo() == null ? "se pisa con otra función" : f.motivo());
            texto.append('\n');
        }
        return texto.toString();
    }
}
