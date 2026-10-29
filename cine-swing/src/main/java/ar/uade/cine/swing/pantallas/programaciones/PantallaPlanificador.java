package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.dto.programaciones.IndicadoresGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PedidoGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PropuestaGrilla;
import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;

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

import static ar.uade.cine.swing.comun.Formato.hora;

// El planificador de la semana: el backend elige elenco y pases; acá solo se piden, se comparan y se aplican.
// Determinista: lo que muestra Previsualizar es lo que crea Aplicar.
public final class PantallaPlanificador extends Pantalla {

    private record Idiomas(List<String> idiomas, List<String> proyecciones) {
    }

    // Estático y no de la pantalla: comparar corridas es el uso normal y sobrevive a salir y volver.
    private static IndicadoresGrilla corridaAnterior;

    private final ApiCatalogos apiCatalogos;
    private final ApiProgramaciones apiProgramaciones;
    private final CampoFecha desde = new CampoFecha(LocalDate.now());
    private final JTextField dias = Campos.soloEntero(new JTextField("7"));
    private final JSpinner apertura = Fechas.hora(LocalTime.of(14, 0));
    private final JSpinner cierre = Fechas.hora(LocalTime.MIDNIGHT);
    private final JTextField cuantasPeliculas = Campos.soloEntero(new JTextField("8"));
    private final JTextField precioBase = Campos.soloDecimal(new JTextField("5000"));
    private final JLabel error = Componentes.texto(" ");
    private final JComboBox<Opcion<String>> idioma = new JComboBox<>();
    private final JComboBox<Opcion<String>> proyeccion = new JComboBox<>();
    private final JButton previsualizar = new JButton("Previsualizar");
    private final JButton aplicar = Componentes.primario(new JButton("Aplicar"));
    private final ResultadoGrilla resultado = new ResultadoGrilla();
    private PropuestaGrilla propuesta;
    // Cada cambio de criterio sube la versión: una respuesta de criterios viejos se descarta al llegar.
    private int version;

    public PantallaPlanificador(ApiCatalogos apiCatalogos, ApiProgramaciones apiProgramaciones) {
        super("Planificador de la semana", "Arma la semana con las películas confirmadas: elige por puntaje y "
                + "variedad de géneros, y le da más funciones a la mejor. No pisa lo ya programado.");
        this.apiCatalogos = apiCatalogos;
        this.apiProgramaciones = apiProgramaciones;

        add(criterios(), BorderLayout.WEST);
        add(resultado, BorderLayout.CENTER);
        cargar(() -> new Idiomas(apiCatalogos.obtenerIdiomas(), apiCatalogos.obtenerProyecciones()), c -> {
            Campos.llenar(idioma, Opciones.etiquetadas(c.idiomas()));
            Campos.llenar(proyeccion, Opciones.etiquetadas(c.proyecciones()));
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
                .ancho(Componentes.subtitulo("Cuándo"))
                .campo("Desde", desde)
                .campo("Días", dias)
                .campo("Apertura", apertura)
                .campo("Cierre", cierre)
                .ancho(Componentes.nota("A esa hora tiene que haber terminado la última función."))
                .ancho(Componentes.subtitulo("Qué"))
                .campo("Películas", cuantasPeliculas)
                .obligatorio("Precio base", precioBase)
                .campo("Idioma", idioma)
                .campo("Proyección", proyeccion)
                .ancho(botones)
                .ancho(error)
                .ancho(Componentes.nota("Previsualizar no crea nada."))
                .cerrar();

        Campos.alCambiar(this::criteriosCambiados, desde, apertura, cierre, idioma, proyeccion, dias,
                cuantasPeliculas, precioBase);

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
        Integer cuantas = v.entero(cuantasPeliculas, "Películas", false);
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
            resultado.mostrar(grilla, anterior, pedido.cuantasPeliculas());
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
