package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.dto.programaciones.IndicadoresGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PedidoGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PropuestaGrilla;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.hora;

/**
 * Planificador de la semana: el backend elige el elenco y reparte los pases; acá solo se piden y se muestran.
 * Determinista: lo que muestra Previsualizar es exactamente lo que crea Aplicar.
 */
public final class PantallaPlanificador extends Pantalla {

    private record Idiomas(List<String> idiomas, List<String> proyecciones) {
    }

    // Estático y no de la pantalla: comparar corridas es el uso normal y sobrevive a salir y volver.
    private static IndicadoresGrilla corridaAnterior;

    private final ApiCatalogos apiCatalogos;
    private final ApiProgramaciones apiProgramaciones;
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JTextField dias = Campos.soloEntero(new JTextField("7"));
    private final JSpinner apertura = Fechas.hora(LocalTime.of(14, 0));
    private final JSpinner cierre = Fechas.hora(LocalTime.MIDNIGHT);
    private final JTextField cuantasPeliculas = Campos.soloEntero(new JTextField("8"));
    private final JTextField precioBase = Campos.soloDecimal(new JTextField("5000"));
    private final JLabel error = Componentes.texto(" ");
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JButton previsualizar = new JButton("Previsualizar");
    private final JButton aplicar = new JButton("Aplicar");
    private final ResultadoGrilla resultado = new ResultadoGrilla();
    private PropuestaGrilla propuesta;
    // Cada cambio de criterio sube la versión: una respuesta de criterios viejos se descarta al llegar.
    private int version;

    public PantallaPlanificador(ApiCatalogos apiCatalogos, ApiProgramaciones apiProgramaciones) {
        super("Planificador de la semana", "Elige el elenco con un criterio que mira <b>puntaje y géneros a la "
                + "vez</b> y reparte los pases entre las salas de forma proporcional al puntaje: la mejor de la semana "
                + "se lleva cuatro o cinco funciones diarias y la última, una. No pisa funciones ya cargadas.");
        this.apiCatalogos = apiCatalogos;
        this.apiProgramaciones = apiProgramaciones;

        add(criterios(), BorderLayout.WEST);
        add(resultado, BorderLayout.CENTER);
        cargar(() -> new Idiomas(apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), c -> {
            Opcion.de(c.idiomas(), v -> etiqueta(v)).forEach(idioma::addItem);
            Opcion.de(c.proyecciones(), v -> etiqueta(v)).forEach(proyeccion::addItem);
            version++;
        });
    }

    private JScrollPane criterios() {
        previsualizar.addActionListener(e -> correr(false));
        aplicar.addActionListener(e -> correr(true));
        aplicar.setEnabled(false);
        JPanel botones = new JPanel(new GridLayout(2, 1, 0, 6));
        botones.add(previsualizar);
        botones.add(aplicar);

        Formulario formulario = new Formulario()
                .ancho(Componentes.subtitulo("Criterios"))
                .campo("Desde", desde)
                .campo("Días", dias)
                .campo("Apertura", apertura)
                .campo("Cierre", cierre)
                .ancho(Componentes.nota("El cierre a las 00:00 se lee como el final del día. Es hasta cuándo tiene "
                        + "que <i>haber terminado</i> la última función, no cuándo puede empezar."))
                .campo("Cuántas películas", cuantasPeliculas)
                .ancho(Componentes.nota("Títulos distintos en la semana. Solo se eligen entre las confirmadas."))
                .obligatorio("Precio base", precioBase)
                .campo("Idioma", idioma)
                .campo("Proyección", proyeccion)
                .ancho(botones)
                .ancho(error)
                .ancho(Componentes.nota("Solo el precio es obligatorio: lo que quede vacío lo completa el servidor "
                        + "con su valor por defecto (una semana desde hoy, ocho títulos)."))
                .ancho(Componentes.nota("Previsualizar no escribe nada. Al aplicar, el servidor <b>vuelve a "
                        + "calcular</b> la propuesta con estos mismos criterios: si alguien programó algo en el "
                        + "medio, lo respeta."))
                .cerrar();

        Runnable cambio = this::criteriosCambiados;
        desde.addPropertyChangeListener("date", e -> cambio.run());
        apertura.addChangeListener(e -> cambio.run());
        cierre.addChangeListener(e -> cambio.run());
        idioma.addActionListener(e -> cambio.run());
        proyeccion.addActionListener(e -> cambio.run());
        Campos.alCambiar(dias, cambio);
        Campos.alCambiar(cuantasPeliculas, cambio);
        Campos.alCambiar(precioBase, cambio);

        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    private void criteriosCambiados() {
        version++;
        if (propuesta == null) return;
        propuesta = null;
        aplicar.setText("Aplicar");
        aplicar.setEnabled(false);
        resultado.vaciar();
    }

    // Solo el precio es obligatorio: lo que va null lo completa el backend con su default.
    private PedidoGrilla pedido(Validacion v) {
        String inicio = v.fecha(desde, "Desde", false);
        Integer cuantosDias = v.entero(dias, "Días", false);
        Integer cuantas = v.entero(cuantasPeliculas, "Cuántas películas", false);
        Double precio = v.decimal(precioBase, "Precio base", true);
        v.alMencionar("películas", cuantasPeliculas);
        if (!v.ok()) return null;
        return new PedidoGrilla(inicio, cuantosDias, Fechas.leerHora(apertura).toString(),
                Fechas.leerHora(cierre).toString(), cuantas, precio, Campos.elegido(idioma),
                Campos.elegido(proyeccion));
    }

    private void correr(boolean aplicando) {
        Validacion v = new Validacion(error);
        PedidoGrilla pedido = pedido(v);
        if (pedido == null) return;
        int pedidaEn = version;
        previsualizar.setEnabled(false);
        aplicar.setEnabled(false);
        resultado.esperar(aplicando);
        Tarea.ejecutar(this, () -> aplicando ? apiProgramaciones.armarGrilla(pedido)
                : apiProgramaciones.proponerGrilla(pedido), grilla -> {
            previsualizar.setEnabled(true);
            if (pedidaEn != version) return;
            IndicadoresGrilla anterior = corridaAnterior;
            corridaAnterior = grilla.indicadores();
            propuesta = grilla;
            // funcionesCreadas es 0 al previsualizar: es lo único que distingue «así quedaría» de «así quedó».
            boolean aplicada = grilla.funcionesCreadas() > 0;
            aplicar.setText(aplicada ? "Aplicada" : "Crear " + grilla.pases().size() + " funciones");
            aplicar.setEnabled(!aplicada && !grilla.pases().isEmpty());
            resultado.mostrar(grilla, anterior);
            if (aplicando) avisar("Se crearon " + grilla.funcionesCreadas() + " funciones");
        }, e -> {
            previsualizar.setEnabled(true);
            propuesta = null;
            aplicar.setText("Aplicar");
            // El motivo va junto a los criterios, que es lo que hay que cambiar.
            resultado.vaciar();
            v.mostrarError(e);
        });
    }
}
