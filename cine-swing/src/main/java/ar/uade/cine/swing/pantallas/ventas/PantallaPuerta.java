package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.ItemCompra;
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
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.escapar;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

// Control de acceso (CU-18) y entrega del candy con el mismo QR (D7); es lo que ve el acomodador.
public final class PantallaPuerta extends Pantalla {

    private final ApiVentas apiVentas;
    private final ApiCandy apiCandy;
    private final JTextField codigo = new JTextField(10);
    private final JLabel resultado = new JLabel();
    private final JLabel error = Componentes.texto(" ");
    private static final float LETRA_PUERTA = 18f;

    public PantallaPuerta(ApiVentas apiVentas, ApiCandy apiCandy) {
        super("Validar entrada", "Escaneá el código del ticket o tipealo. Cada entrada sirve una sola vez, "
                + "y su candy se entrega una vez.");
        this.apiVentas = apiVentas;
        this.apiCandy = apiCandy;

        codigo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        // Letra grande a propósito: la Puerta se usa de pie y de lejos.
        JButton validar = Componentes.primario(new JButton("Validar"));
        validar.setFont(validar.getFont().deriveFont(LETRA_PUERTA));
        JButton entregar = new JButton("Entregar candy");
        entregar.setFont(entregar.getFont().deriveFont(LETRA_PUERTA));
        JPanel fila = new JPanel(new FlujoConSalto());
        fila.add(codigo);
        fila.add(validar);
        fila.add(entregar);

        // Arriba y del alto de lo que dice: el veredicto se lee al lado del código, no al pie de la ventana.
        Pila columna = new Pila();
        columna.agregar(fila, error).espacio(Escala.APARTE).agregar(resultado);
        add(Componentes.arriba(columna), BorderLayout.CENTER);

        codigo.addActionListener(e -> validar());
        validar.addActionListener(e -> validar());
        entregar.addActionListener(e -> entregar());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        codigo.requestFocusInWindow();
    }

    private void validar() {
        pedir(apiVentas::validarEntrada, this::mostrarValida, "NO PASA");
    }

    private void entregar() {
        pedir(apiCandy::retirarCandy, this::mostrarCandy, "NO SE ENTREGA");
    }

    // El foco vuelve al campo tras cada pedido, con el código elegido: el próximo escaneo lo reemplaza, y
    // mientras tanto sirve para entregar el candy de la misma entrada sin volver a tipearlo.
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
        mostrar(Colores.exito(), "ADELANTE", html.toString());
    }

    // La web vende el candy con la entrada: la Puerta avisa si hay que entregarlo, como lo dice el ticket.
    private static String candyDe(Reserva reserva) {
        List<CompraCandy> aRetirar = reserva.candy().stream().filter(c -> "A retirar".equals(c.situacion())).toList();
        if (aRetirar.isEmpty()) return "";
        String items = aRetirar.stream().flatMap(c -> c.items().stream())
                .map(i -> i.cantidad() + "× " + escapar(i.nombre())).collect(Collectors.joining(", "));
        return "<p><b>Candy a retirar:</b> " + items + " · apretá «Entregar candy»</p>";
    }

    private void mostrarCandy(List<CompraCandy> compras) {
        StringBuilder html = new StringBuilder("<table>");
        for (CompraCandy compra : compras) {
            for (ItemCompra item : compra.items()) {
                html.append("<tr><td><b>").append(item.cantidad()).append(" ×</b></td><td>")
                        .append(escapar(item.nombre())).append("</td></tr>");
            }
        }
        html.append("</table>");
        mostrar(Colores.exito(), "ENTREGAR", html.toString());
    }

    private void mostrar(Color color, String titulo, String cuerpo) {
        resultado.setText("<html><div style='width:460px'><h1 style='color:" + Colores.hex(color) + "'>" + titulo
                + "</h1>" + cuerpo + "</div></html>");
        resultado.setBorder(new FlatLineBorder(new Insets(Escala.CERCA, Escala.APARTE, Escala.CERCA,
                Escala.APARTE), color, 2, Escala.ARCO));
    }
}
