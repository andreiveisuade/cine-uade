package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.CompraCandy.Situacion;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;
import com.formdev.flatlaf.FlatClientProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.escapar;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

// Control de acceso (CU-18) y entrega del candy (D7) con el mismo QR, en dos modos; es lo que ve el acomodador.
// Un modo por canje y un solo botón: con dos botones sueltos, el Enter del lector siempre validaba la entrada.
public final class PantallaPuerta extends Pantalla {

    /** En las Preferences: el último modo elegido, porque cada puesto (la sala o el candy) usa casi siempre uno. */
    static final String CLAVE_MODO = "puerta.modo";

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    enum Modo {
        ENTRADA("Entrada", "Validar entrada"), CANDY("Candy", "Entregar candy");

        private final String titulo;
        private final String accion;
    }

    private static final float LETRA_PUERTA = 18f;

    private final ApiVentas apiVentas;
    private final ApiCandy apiCandy;
    private final Preferences preferencias;
    private final JTextField codigo = new JTextField(10);
    private final JButton accion = Componentes.primario(new JButton());
    private final JLabel resultado = new JLabel();
    private final JLabel error = Componentes.texto(" ");
    private Modo modo;

    public PantallaPuerta(ApiVentas apiVentas, ApiCandy apiCandy, Preferences preferencias) {
        super("Puerta", "Elegí qué se canjea y escaneá el código del ticket o tipealo. El mismo código "
                + "entra a la sala una vez y retira su candy una vez, por separado.");
        this.apiVentas = apiVentas;
        this.apiCandy = apiCandy;
        this.preferencias = preferencias;
        String guardado = preferencias.get(CLAVE_MODO, Modo.ENTRADA.name());
        this.modo = Arrays.stream(Modo.values()).filter(m -> m.name().equals(guardado)).findFirst()
                .orElse(Modo.ENTRADA);

        codigo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        // Letra grande a propósito: la Puerta se usa de pie y de lejos.
        accion.setFont(accion.getFont().deriveFont(LETRA_PUERTA));
        JPanel fila = new JPanel(new FlujoConSalto());
        fila.add(codigo);
        fila.add(accion);

        // Arriba y del alto de lo que dice: el veredicto se lee al lado del código, no al pie de la ventana.
        Pila columna = new Pila();
        columna.agregar(selector(), fila, error).espacio(Escala.APARTE).agregar(resultado);
        add(Componentes.arriba(columna), BorderLayout.CENTER);

        codigo.addActionListener(e -> canjear());
        accion.addActionListener(e -> canjear());
        usar(modo);
    }

    // Pestañas como las del menú: se ve de lejos cuál está elegido, y el texto del botón lo repite.
    private JPanel selector() {
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ButtonGroup grupo = new ButtonGroup();
        for (Modo m : Modo.values()) {
            JToggleButton boton = new JToggleButton(m.titulo(), m == modo);
            boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TAB);
            boton.setFont(boton.getFont().deriveFont(Font.BOLD, LETRA_PUERTA));
            boton.addActionListener(e -> {
                usar(m);
                preferencias.put(CLAVE_MODO, m.name());
                reiniciar();
            });
            grupo.add(boton);
            selector.add(boton);
        }
        return selector;
    }

    private void usar(Modo nuevo) {
        modo = nuevo;
        accion.setText(nuevo.accion());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        codigo.requestFocusInWindow();
    }

    private void canjear() {
        switch (modo) {
            case ENTRADA -> pedir(apiVentas::validarEntrada, this::mostrarValida, "ENTRADA NO VÁLIDA");
            case CANDY -> pedir(apiCandy::retirarCandy, this::mostrarCandy, "CANDY NO ENTREGADO");
        }
    }

    // El foco vuelve al campo tras cada pedido, con el código elegido: el próximo escaneo lo reemplaza, y
    // mientras tanto sirve para canjear en el otro modo la misma entrada sin volver a tipearlo.
    private <T> void pedir(Function<String, T> pedido, Consumer<T> mostrarBien, String rechazo) {
        Validacion v = new Validacion(error);
        String tipeado = v.texto(codigo, "Código", true);
        if (!v.ok()) return;
        Tarea.ejecutar(this, () -> pedido.apply(tipeado), respuesta -> {
            mostrarBien.accept(respuesta);
            reiniciar();
        }, error -> {
            // Sin conexión o con el servidor caído no se sabe si pasa: error global, no un rechazo.
            if (error.esSesionVencida()) return;
            if (!error.esDelFormulario() && !error.esConflicto()) {
                Mensajes.error(this, error);
                reiniciar();
                return;
            }
            mostrar(Colores.error(), rechazo, "<p>" + escapar(error.getMessage()) + "</p>");
            reiniciar();
        });
    }

    private void reiniciar() {
        codigo.selectAll();
        codigo.requestFocusInWindow();
    }

    private void mostrarValida(Reserva reserva) {
        StringBuilder html = new StringBuilder();
        html.append("<p><b>").append(reserva.pelicula() == null ? "" : escapar(reserva.pelicula().titulo()))
                .append("</b><br>").append(reserva.sala() == null ? "" : escapar(reserva.sala().nombre()))
                .append(" · ").append(reserva.funcion() == null ? "" : fechaHora(reserva.funcion().inicio()))
                .append("</p><table>");
        for (Entrada e : reserva.entradas()) {
            String tarifa = e.tarifa();
            html.append("<tr><td><tt><b>").append(e.codigo()).append("</b></tt></td><td>")
                    .append(e.requiereAcreditacion() ? "<b>" + etiqueta(tarifa) + " · pedir carnet</b>" : etiqueta(tarifa))
                    .append("</td></tr>");
        }
        int personas = reserva.entradas().size();
        html.append("</table><p>").append(cantidad(personas, "persona", "personas"))
                .append(" · ingreso registrado ").append(fechaHora(reserva.ingresadaEn())).append("</p>");
        html.append(candyDe(reserva));
        mostrar(Colores.exito(), "ENTRADA VÁLIDA", html.toString());
    }

    // Avisa sin entregarlo: el retiro es otro canje y lo registra el modo Candy.
    private static String candyDe(Reserva reserva) {
        List<CompraCandy> aRetirar = reserva.candy().stream()
                .filter(c -> c.situacion() == Situacion.A_RETIRAR).toList();
        if (aRetirar.isEmpty()) return "";
        return "<p><b>Tiene candy para retirar:</b> pasá a modo Candy (" + items(aRetirar) + ")</p>";
    }

    private void mostrarCandy(List<CompraCandy> compras) {
        mostrar(Colores.exito(), "CANDY ENTREGADO: " + items(compras), "");
    }

    private static String items(List<CompraCandy> compras) {
        return compras.stream().flatMap(c -> c.items().stream())
                .map(i -> i.cantidad() + " × " + escapar(i.nombre())).collect(Collectors.joining(", "));
    }

    private void mostrar(Color color, String titulo, String cuerpo) {
        resultado.setText("<html><div style='width:460px'><h1 style='color:" + Colores.hex(color) + "'>" + titulo
                + "</h1>" + cuerpo + "</div></html>");
        resultado.setBorder(Componentes.redondeado(new Insets(Escala.CERCA, Escala.APARTE, Escala.CERCA,
                Escala.APARTE), color, 2, Escala.ARCO));
    }
}
