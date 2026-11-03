package ar.uade.cine.swing.pantallas.pedidos;

import ar.uade.cine.swing.api.RegistroDePedidos;
import ar.uade.cine.swing.api.RegistroDePedidos.Pedido;
import ar.uade.cine.swing.api.RegistroDePedidos.Resultado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// El panel plegable de abajo: qué se está cargando y cómo terminó cada pedido a la API; Observer del registro.
public final class PanelDePedidos extends JPanel {

    private static final int ALTO = 260;
    private static final int ANCHO_DETALLE = 340;
    private static final int REFRESCO_MS = 500;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SIN_ELEGIR = "Elegí un pedido para ver el detalle";

    private final RegistroDePedidos registro;
    private final JToggleButton boton = new JToggleButton("Pedidos");
    private final JLabel resumen = new JLabel(" ");
    private final JTextArea detalle = new JTextArea(SIN_ELEGIR);
    private final Tabla<Pedido> tabla = new Tabla<Pedido>(
            Columna.<Pedido>de("Hora", p -> p.hora().format(HORA)).ancho(70),
            Columna.<Pedido>de("Método", Pedido::metodo).ancho(60),
            Columna.<Pedido>de("Ruta", Pedido::ruta).ancho(420),
            Columna.<Pedido>de("Estado", PanelDePedidos::estado).comoEstado(PanelDePedidos::color).ancho(90),
            Columna.<Pedido>numero("Tamaño", p -> p.enCurso() ? "" : tamano(p.resultado().recibidos())).ancho(70),
            Columna.<Pedido>numero("Duración", p -> p.duracionMs() + " ms").ancho(80))
            .vacia("Todavía no salió ningún pedido");
    // Mientras algo carga, la duración sigue corriendo aunque no llegue ningún aviso nuevo.
    private final Timer reloj = new Timer(REFRESCO_MS, e -> refrescar());
    private Long elegido;
    private boolean refrescando;

    public PanelDePedidos(RegistroDePedidos registro) {
        super(new BorderLayout(Escala.JUNTO, Escala.PEGADO));
        this.registro = registro;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Colores.borde()),
                BorderFactory.createEmptyBorder(Escala.JUNTO, Escala.APARTE, Escala.JUNTO, Escala.APARTE)));
        setPreferredSize(new Dimension(0, ALTO));
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(false);
        detalle.setBorder(BorderFactory.createEmptyBorder(Escala.JUNTO, Escala.JUNTO, Escala.JUNTO, Escala.JUNTO));
        JScrollPane conDetalle = new JScrollPane(detalle);
        conDetalle.setPreferredSize(new Dimension(ANCHO_DETALLE, 0));
        add(resumen, BorderLayout.NORTH);
        add(tabla.conScroll(), BorderLayout.CENTER);
        add(conDetalle, BorderLayout.EAST);
        setVisible(false);
        boton.setToolTipText("Mostrar u ocultar los pedidos al servidor");
        boton.addActionListener(e -> {
            setVisible(boton.isSelected());
            revalidate();
        });
        tabla.tabla().getSelectionModel().addListSelectionListener(e -> {
            // Rearmar las filas borra la selección: esa no la hizo el encargado.
            if (refrescando || e.getValueIsAdjusting()) return;
            elegido = tabla.seleccionada().map(Pedido::id).orElse(null);
            mostrarDetalle(registro.recientes());
        });
        registro.alCambiar(() -> SwingUtilities.invokeLater(this::refrescar));
        refrescar();
    }

    public JToggleButton boton() {
        return boton;
    }

    Tabla<Pedido> tabla() {
        return tabla;
    }

    String resumen() {
        return resumen.getText();
    }

    String detalle() {
        return detalle.getText();
    }

    private void refrescar() {
        List<Pedido> pedidos = registro.recientes();
        long cargando = pedidos.stream().filter(Pedido::enCurso).count();
        long fallaron = pedidos.stream().filter(Pedido::fallo).count();
        refrescando = true;
        tabla.mostrar(pedidos);
        if (elegido != null) tabla.seleccionar(p -> p.id() == elegido);
        refrescando = false;
        mostrarDetalle(pedidos);
        resumen.setText(cargando + " cargando · " + (pedidos.size() - cargando - fallaron) + " bien · "
                + fallaron + " con error");
        boton.setText(cargando > 0 ? "Pedidos · " + cargando + " cargando" : "Pedidos");
        if (cargando > 0) reloj.start();
        else reloj.stop();
    }

    private void mostrarDetalle(List<Pedido> pedidos) {
        String texto = pedidos.stream().filter(p -> elegido != null && p.id() == elegido).findFirst()
                .map(PanelDePedidos::describir).orElse(SIN_ELEGIR);
        // Sin cambio no se reescribe: si no, cada refresco le borraría al encargado lo que está seleccionando.
        if (!texto.equals(detalle.getText())) detalle.setText(texto);
    }

    private static String describir(Pedido p) {
        StringBuilder texto = new StringBuilder(p.metodo()).append(' ').append(p.url())
                .append("\n\nEstado: ").append(estado(p))
                .append("\nSalió: ").append(p.hora().format(HORA))
                .append("\nDuración: ").append(p.duracionMs()).append(" ms")
                .append("\nEnviado: ").append(tamano(p.enviados()));
        Resultado resultado = p.resultado();
        if (resultado == null) return texto.toString();
        texto.append("\nRecibido: ").append(tamano(resultado.recibidos()));
        if (resultado.tipo() != null) texto.append("\nTipo: ").append(resultado.tipo());
        if (resultado.mensaje() != null) texto.append("\n\nError: ").append(resultado.mensaje());
        return texto.toString();
    }

    private static String estado(Pedido pedido) {
        return pedido.enCurso() ? "Cargando" : pedido.resultado().estado();
    }

    static String tamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.of("es", "AR"), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.of("es", "AR"), "%.1f MB", bytes / (1024.0 * 1024));
    }

    private static Color color(Pedido pedido) {
        if (pedido.enCurso()) return Colores.aviso();
        return pedido.fallo() ? Colores.error() : Colores.exito();
    }
}
