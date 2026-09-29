package ar.uade.cine.swing.pantallas.salas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.catalogos.TipoSala;
import ar.uade.cine.swing.api.dto.salas.PedidoSala;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Lecturas;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Las salas: listado, alta con su distribución de butacas y baja; doble clic abre el mapa de butacas.
public final class PantallaSalas extends Pantalla {

    private record Datos(List<Sala> salas, List<TipoSala> tipos) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiSalas apiSalas;
    private final Navegacion navegacion;
    private final Tabla<Sala> tabla = new Tabla<>(
            Columna.<Sala>de("Sala", Sala::nombre).ancho(160),
            Columna.<Sala>de("Tipo", s -> etiqueta(s.tipo())),
            Columna.<Sala>de("Distribución", s -> s.butacasPorFila().stream().map(String::valueOf)
                    .collect(Collectors.joining(","))).ancho(180),
            Columna.<Sala>numero("Butacas", Sala::capacidadSala),
            Columna.<Sala>numero("Limpieza", s -> s.minutosLimpieza() + " min"));

    private final JTextField nombre = new JTextField();
    private final JComboBox<Opcion<String>> tipo = new JComboBox<>();
    private final JTextField distribucion = Campos.soloListaDeEnteros(new JTextField());
    // Vacío dice cómo se escribe; con filas, las cuenta. El formato no se adivina por el nombre del campo.
    private static final String FORMATO_FILAS = "Las butacas de cada fila, separadas por coma: 10, 10, 12";
    private final JLabel resumenDistribucion = new JLabel(FORMATO_FILAS);
    private final JTextField vip = new JTextField();
    private final JTextField pareja = new JTextField();
    private final JTextField accesibles = new JTextField();
    private final JTextField limpieza = Campos.soloEntero(new JTextField("15"));
    private final JLabel error = Componentes.texto(" ");

    public PantallaSalas(ApiCatalogos apiCatalogos, ApiSalas apiSalas, Navegacion navegacion) {
        super("Salas", "Doble clic en una sala abre su mapa, para marcar butacas fuera de servicio.");
        this.apiCatalogos = apiCatalogos;
        this.apiSalas = apiSalas;
        this.navegacion = navegacion;

        JButton butacas = new JButton("Butacas");
        JButton borrar = new JButton("Borrar");
        butacas.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirMapa));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::borrar));
        tabla.alDobleClic(this::abrirMapa);
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(butacas);
        acciones.add(borrar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(acciones, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(formulario(), BorderLayout.EAST);
        recargar();
    }

    private JScrollPane formulario() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        for (JTextField campo : List.of(distribucion, vip, pareja, accesibles)) campo.setFont(mono);
        distribucion.setToolTipText("Una fila por número, separadas por coma. La primera es la A. Ej: 8,10,12,12,14");
        vip.setToolTipText("Ej: I1,I2,J1");
        pareja.setToolTipText("Ej: A1,A2");
        accesibles.setToolTipText("Ej: A1,A8");
        resumenDistribucion.setForeground(Colores.secundario());
        Campos.alCambiar(distribucion, this::resumir);
        JButton crear = new JButton("Crear sala");
        crear.addActionListener(e -> crear());

        Formulario formulario = new Formulario()
                .ancho(Componentes.subtitulo("Nueva sala"))
                .obligatorio("Nombre", nombre)
                .obligatorio("Tipo", tipo)
                .obligatorio("Butacas por fila", distribucion)
                .ancho(resumenDistribucion)
                .campo("Butacas VIP", vip)
                .campo("Butacas de pareja", pareja)
                .campo("Butacas accesibles", accesibles)
                .ancho(Componentes.nota("Códigos separados por coma, como A1, A2. Cada butaca va en una sola lista."))
                .campo("Minutos de limpieza", limpieza)
                .ancho(Componentes.nota("Lo que hay que esperar entre dos funciones. Una sala chica se levanta más "
                        + "rápido."))
                .ancho(crear)
                .ancho(error)
                .cerrar();
        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    // Cuenta filas y butacas mientras se tipea: es aritmética sobre lo tipeado, no una regla.
    private void resumir() {
        List<Integer> filas = Lecturas.leerEnteros(distribucion.getText(), "", false).valor();
        if (filas == null || filas.isEmpty()) {
            resumenDistribucion.setText(FORMATO_FILAS);
            return;
        }
        resumenDistribucion.setText(filas.size() + " filas (A–" + (char) ('A' + filas.size() - 1) + "), "
                + filas.stream().mapToInt(Integer::intValue).sum() + " butacas");
    }

    private void recargar() {
        cargar(() -> new Datos(apiSalas.obtenerSalas(), apiCatalogos.obtenerTiposSala()), datos -> {
            tabla.mostrar(datos.salas());
            if (tipo.getItemCount() == 0) {
                datos.tipos().forEach(t -> tipo.addItem(new Opcion<>(t.nombre(),
                        etiqueta(t.nombre()) + " (×" + t.multiplicador() + ")")));
            }
        });
    }

    private void crear() {
        Validacion v = new Validacion(error);
        String nombreLeido = v.texto(nombre, "Nombre", true);
        String tipoElegido = v.elegido(tipo, "Tipo");
        List<Integer> filas = v.enteros(distribucion, "Butacas por fila", true);
        List<String> codigosVip = v.codigos(vip, "Butacas VIP");
        List<String> codigosPareja = v.codigos(pareja, "Butacas de pareja");
        List<String> codigosAccesibles = v.codigos(accesibles, "Butacas accesibles");
        // Vacío viaja null: el backend usa sus 15 minutos por defecto.
        Integer minutos = v.entero(limpieza, "Minutos de limpieza", false);
        v.alMencionar("fila", distribucion);
        if (!v.ok()) return;
        PedidoSala pedido = new PedidoSala(nombreLeido, tipoElegido, filas, codigosVip, codigosPareja,
                codigosAccesibles, minutos);
        Tarea.ejecutar(this, () -> apiSalas.crearSala(pedido), creada -> {
            avisar(creada.nombre() + " creada con " + creada.capacidadSala() + " butacas");
            for (JTextField campo : List.of(nombre, distribucion, vip, pareja, accesibles)) campo.setText("");
            limpieza.setText("15");
            recargar();
        }, v::mostrarError);
    }

    private void borrar(Sala sala) {
        if (!confirmar("¿Borrar " + sala.nombre() + "?", "Sí, borrar")) return;
        accion(() -> apiSalas.eliminarSala(sala.id()), "Sala borrada", this::recargar);
    }

    private void abrirMapa(Sala sala) {
        navegacion.abrirMapa(sala.id());
    }
}
