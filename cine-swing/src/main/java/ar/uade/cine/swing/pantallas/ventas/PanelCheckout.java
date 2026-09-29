package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Consumer;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// Un checkout abierto en la pasarela emulada: el QR, el link y el botón que confirma que el cliente pagó.
/** {@code codigoQr} es el contenido y no una imagen: la pasarela es emulada y dibujarlo la haría parecer real. */
final class PanelCheckout extends Seccion {

    PanelCheckout(ApiVentas apiVentas, Checkout checkout, Consumer<Pago> alCobrar) {
        super(new BorderLayout());
        Pila panel = new Pila();
        panel.agregar(Componentes.subtitulo("Checkout abierto · " + etiqueta(checkout.medio())));
        panel.agregar(Componentes.nota(checkout.id()));
        panel.agregar(new JLabel("El cliente aprueba"));
        JLabel monto = new JLabel(precio(checkout.monto()));
        monto.setFont(monto.getFont().deriveFont(Font.BOLD, 22f));
        panel.agregar(monto);
        panel.agregar(new JLabel("Contenido del QR"));
        panel.agregar(texto(checkout.codigoQr()));
        panel.agregar(new JLabel("Link de pago"));
        panel.agregar(texto(checkout.urlPago()));
        JButton confirmar = new JButton("El cliente pagó · confirmar");
        confirmar.addActionListener(e -> {
            if (!confirmar("¿El cliente aprobó el pago de " + precio(checkout.monto()) + "? Se registra "
                    + "el cobro y no se puede deshacer.", "Sí, confirmar el pago")) return;
            confirmar.setEnabled(false);
            // Qué se está pagando sale del checkout, no de quien confirma.
            Tarea.ejecutar(this, () -> apiVentas.confirmarCheckout(checkout.id()), alCobrar, error -> {
                confirmar.setEnabled(true);
                Mensajes.error(this, error);
            });
        });
        panel.agregar(confirmar);
        panel.agregar(Componentes.nota("El monto <b>ya tiene el descuento aplicado</b>: es el "
                + "importe que se aprueba. La pasarela es una emulación —el host no existe—, así que el aviso de "
                + "«pagó» lo da esta pantalla; en una integración de verdad lo dispara el procesador."));
        add(Componentes.conBorde(panel));
    }

    private static JTextArea texto(String valor) {
        JTextArea area = new JTextArea(valor == null ? "" : valor);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }
}
