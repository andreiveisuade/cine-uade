package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// El lado del cobro: medio de pago y el botón que cobra en caja o abre el checkout, según pida el catálogo.
final class PanelCobro extends Seccion {

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final Reserva reserva;
    private final List<MedioPago> medios;
    private final JComboBox<Opcion<String>> medio = new JComboBox<>();
    private final JLabel explicacion = Componentes.nota("");
    private final JButton enviar = new JButton();
    private final JLabel error = Componentes.texto(" ");
    private final JPanel checkout = new JPanel(new BorderLayout());

    PanelCobro(ApiVentas apiVentas, Navegacion navegacion, Reserva reserva, List<MedioPago> medios) {
        super(new BorderLayout());
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;
        this.reserva = reserva;
        this.medios = medios;
        medios.forEach(m -> medio.addItem(new Opcion<>(m.nombre(), etiqueta(m.nombre()))));

        JLabel total = new JLabel(precio(reserva.total()));
        total.setFont(total.getFont().deriveFont(Font.BOLD, 22f));

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.add(Componentes.izquierda(Componentes.subtitulo("Cobro")));
        contenido.add(Componentes.izquierda(new JLabel("Medio de pago *")));
        medio.setMaximumSize(new Dimension(Integer.MAX_VALUE, medio.getPreferredSize().height));
        contenido.add(Componentes.izquierda(medio));
        contenido.add(Componentes.izquierda(new JLabel(" ")));
        contenido.add(Componentes.izquierda(new JLabel("A cobrar")));
        contenido.add(Componentes.izquierda(total));
        contenido.add(Componentes.izquierda(Componentes.nota("Sale del total de las butacas: no se puede cobrar "
                + "otro importe. Si hay una promoción vigente para este medio de pago, el descuento se aplica al "
                + "cobrar.")));
        contenido.add(Componentes.izquierda(new JLabel(" ")));
        contenido.add(Componentes.izquierda(explicacion));
        contenido.add(Componentes.izquierda(enviar));
        contenido.add(Componentes.izquierda(error));
        contenido.add(Componentes.izquierda(checkout));
        add(Componentes.conBorde(contenido), BorderLayout.NORTH);

        // Un checkout es de un medio y un monto concretos: cambiar el medio lo invalida.
        medio.addActionListener(e -> {
            mostrarCheckout(null);
            refrescar();
        });
        enviar.addActionListener(e -> enviar());
        refrescar();
    }

    // R11: en un medio electrónico la autorización la devuelve el procesador, no se tipea.
    private boolean porCheckout() {
        String elegido = Campos.elegido(medio);
        return medios.stream().anyMatch(m -> m.nombre().equals(elegido) && m.requiereAutorizacion());
    }

    private void refrescar() {
        boolean electronico = porCheckout();
        enviar.setText(electronico ? "Abrir checkout" : "Registrar cobro");
        explicacion.setText("<html>" + (electronico
                ? "El cliente paga en la pasarela y el código de autorización lo devuelve ella."
                : "Se cobra en la caja del cine. El efectivo no lleva código de autorización.") + "</html>");
    }

    private void enviar() {
        Validacion v = new Validacion(error);
        String elegido = v.elegido(medio, "Medio de pago");
        if (!v.ok()) return;
        Consumer<ErrorApi> fallo = e -> {
            enviar.setEnabled(true);
            v.mostrarError(e);
        };
        if (!porCheckout()) {
            // Un cobro registrado no se deshace: se pregunta antes. Abrir el checkout, en cambio, no cobra.
            if (!confirmar("¿Registrar el cobro de la reserva #" + reserva.id() + " en "
                    + etiqueta(elegido).toLowerCase() + "? No se puede deshacer.", "Sí, cobrar")) return;
            enviar.setEnabled(false);
            Tarea.ejecutar(this, () -> apiVentas.cobrar(reserva.id(), elegido, ""), this::cobrado, fallo);
        } else {
            // Abrir el checkout valida R5, R17 y R19 antes de mandar a pagar: si no, hay plata que devolver.
            enviar.setEnabled(false);
            Tarea.ejecutar(this, () -> apiVentas.abrirCheckout(reserva.id(), elegido), c -> {
                enviar.setEnabled(true);
                mostrarCheckout(c);
            }, fallo);
        }
    }

    private void cobrado(Pago pago) {
        avisar(pago.descuento() > 0
                ? "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()) + " · "
                + precio(pago.descuento()) + " de descuento"
                : "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()));
        navegacion.ir(Destino.CAJA);
    }

    /** Sin checkout, el lugar queda vacío. */
    private void mostrarCheckout(Checkout abierto) {
        checkout.removeAll();
        if (abierto != null) checkout.add(new PanelCheckout(apiVentas, abierto, this::cobrado));
        checkout.revalidate();
        checkout.repaint();
    }
}
