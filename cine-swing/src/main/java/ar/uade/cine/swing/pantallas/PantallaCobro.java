package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.Checkout;
import ar.uade.cine.swing.api.dto.Entrada;
import ar.uade.cine.swing.api.dto.MedioPago;
import ar.uade.cine.swing.api.dto.Pago;
import ar.uade.cine.swing.api.dto.Reserva;
import ar.uade.cine.swing.api.dto.Tarifa;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/**
 * Cobro de una reserva. El importe no se tipea: sale del total de las butacas y el descuento lo resuelve el backend al
 * cobrar, porque depende del medio de pago.
 */
final class PantallaCobro extends Pantalla {

    private record Datos(Reserva reserva, List<MedioPago> medios, List<Tarifa> tarifas) {
    }

    private final Navegacion navegacion;
    private final int reservaId;
    private final JPanel cuerpo = new JPanel(new BorderLayout());

    PantallaCobro(ApiHttp api, Navegacion navegacion, int reservaId) {
        super(api, "Cobrar reserva #" + reservaId, null);
        this.navegacion = navegacion;
        this.reservaId = reservaId;

        JButton volver = new JButton("← Reservas");
        volver.addActionListener(e -> navegacion.ir("Reservas"));
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        JPanel fila = new JPanel(new BorderLayout());
        fila.add(volver, BorderLayout.WEST);
        centro.add(fila, BorderLayout.NORTH);
        centro.add(cuerpo, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // GET /api/reservas/{id} trae lo mismo embebido que el listado. Un 404 se muestra en la pantalla, no en un
        // diálogo: sin reserva no hay nada más que hacer acá.
        Tarea.ejecutar(this, () -> new Datos(api.obtenerReserva(reservaId), api.obtenerMediosPago(),
                api.obtenerTarifas()), this::pintar, error -> {
            if (!error.esSesionVencida()) mostrarNota(error.getMessage());
        });
    }

    private void mostrarNota(String texto) {
        cuerpo.removeAll();
        cuerpo.add(Componentes.nota(texto), BorderLayout.NORTH);
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    private void pintar(Datos datos) {
        Reserva reserva = datos.reserva();
        if (!"RESERVADA".equals(reserva.estado())) {
            mostrarNota(yaNoSeCobra(reserva));
            return;
        }
        cuerpo.removeAll();
        JPanel columnas = new JPanel(new GridLayout(1, 2, 16, 0));
        columnas.add(new PanelCobro(reserva, datos.medios()));
        columnas.add(detalle(reserva, datos.tarifas()));
        cuerpo.add(columnas, BorderLayout.CENTER);
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    private static String yaNoSeCobra(Reserva reserva) {
        String texto = "La reserva " + reserva.id() + " está " + etiqueta(reserva.estado()).toLowerCase()
                + ", no se puede cobrar.";
        Pago pago = reserva.pago();
        if (pago != null) {
            texto += " Se cobró " + precio(pago.monto()) + " con " + etiqueta(pago.medio());
            if (pago.descuento() > 0) {
                texto += " (subtotal " + precio(pago.subtotal()) + " − " + precio(pago.descuento()) + " de promoción)";
            }
            texto += " el " + fechaHora(pago.fecha()) + ".";
        }
        return texto;
    }

    private JPanel detalle(Reserva reserva, List<Tarifa> tarifas) {
        // Qué tarifa se acredita lo dice el catálogo, no el nombre: una tarifa nueva no obliga a tocar esta pantalla.
        Set<String> seAcreditan = tarifas.stream().filter(Tarifa::requiereAcreditacion).map(Tarifa::nombre)
                .collect(Collectors.toSet());
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.add(Componentes.izquierda(Componentes.subtitulo(
                reserva.pelicula() == null ? "—" : reserva.pelicula().titulo())));
        if (reserva.funcion() != null) {
            arriba.add(Componentes.izquierda(Componentes.nota(dia(reserva.funcion().inicio()) + " "
                    + hora(reserva.funcion().inicio()) + " · " + reserva.sala().nombre() + " ("
                    + etiqueta(reserva.sala().tipo()) + ")")));
        }
        arriba.add(Componentes.izquierda(new JLabel(reserva.cliente() == null ? "—"
                : reserva.cliente().nombre() + "  ·  " + reserva.cliente().email())));
        panel.add(arriba, BorderLayout.NORTH);

        Tabla<Entrada> entradas = new Tabla<>(
                Columna.<Entrada>de("Butaca", Entrada::codigo),
                Columna.<Entrada>de("Tarifa", e -> seAcreditan.contains(e.tarifa())
                        ? etiqueta(e.tarifa()) + " · se acredita en la puerta" : etiqueta(e.tarifa())),
                Columna.<Entrada>numero("Precio", e -> precio(e.precio())));
        entradas.mostrar(reserva.entradas());
        panel.add(entradas.conScroll(), BorderLayout.CENTER);
        JLabel subtotal = new JLabel("Subtotal  " + precio(reserva.total()), JLabel.RIGHT);
        subtotal.setFont(subtotal.getFont().deriveFont(Font.BOLD));
        panel.add(subtotal, BorderLayout.SOUTH);
        return Componentes.conBorde(panel);
    }

    private final class PanelCobro extends JPanel {

        private final Reserva reserva;
        private final List<MedioPago> medios;
        private final JComboBox<Opcion<String>> medio = new JComboBox<>();
        private final JLabel explicacion = Componentes.nota("");
        private final JButton enviar = new JButton();
        private final JPanel checkout = new JPanel(new BorderLayout());

        PanelCobro(Reserva reserva, List<MedioPago> medios) {
            super(new BorderLayout());
            this.reserva = reserva;
            this.medios = medios;
            medios.forEach(m -> medio.addItem(new Opcion<>(m.nombre(), etiqueta(m.nombre()))));

            JLabel total = new JLabel(precio(reserva.total()));
            total.setFont(total.getFont().deriveFont(Font.BOLD, 22f));

            JPanel contenido = new JPanel();
            contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
            agregar(contenido, Componentes.subtitulo("Cobro"));
            agregar(contenido, new JLabel("Medio de pago"));
            medio.setMaximumSize(new Dimension(Integer.MAX_VALUE, medio.getPreferredSize().height));
            agregar(contenido, medio);
            agregar(contenido, new JLabel(" "));
            agregar(contenido, new JLabel("A cobrar"));
            agregar(contenido, total);
            agregar(contenido, Componentes.nota("Sale del total de las butacas: no se puede cobrar otro importe. Si "
                    + "hay una promoción vigente para este medio de pago, el descuento se aplica al cobrar."));
            agregar(contenido, new JLabel(" "));
            agregar(contenido, explicacion);
            agregar(contenido, enviar);
            agregar(contenido, checkout);
            add(Componentes.conBorde(contenido), BorderLayout.NORTH);

            // Un checkout es de un medio y un monto concretos: cambiar el medio lo invalida.
            medio.addActionListener(e -> {
                limpiarCheckout();
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

        private void limpiarCheckout() {
            checkout.removeAll();
            checkout.revalidate();
            checkout.repaint();
        }

        private void enviar() {
            String elegido = Campos.elegido(medio);
            enviar.setEnabled(false);
            if (!porCheckout()) {
                Tarea.ejecutar(this, () -> api.cobrar(reserva.id(), elegido, ""), this::cobrado, this::fallo);
            } else {
                // Abrir el checkout valida R5, R17 y R19 antes de mandar a pagar: si no, hay plata que devolver.
                Tarea.ejecutar(this, () -> api.abrirCheckout(reserva.id(), elegido), c -> {
                    enviar.setEnabled(true);
                    mostrarCheckout(c);
                }, this::fallo);
            }
        }

        private void fallo(ErrorApi error) {
            enviar.setEnabled(true);
            if (!error.esSesionVencida()) Tarea.mostrarError(this, error);
        }

        private void cobrado(Pago pago) {
            avisar(pago.descuento() > 0
                    ? "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()) + " · "
                    + precio(pago.descuento()) + " de descuento"
                    : "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()));
            navegacion.ir("Caja");
        }

        // codigoQr es el contenido y no una imagen: la pasarela es emulada y dibujarlo la haría parecer real.
        private void mostrarCheckout(Checkout c) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            agregar(panel, Componentes.subtitulo("Checkout abierto · " + etiqueta(c.medio())));
            agregar(panel, Componentes.nota(c.id()));
            agregar(panel, new JLabel("El cliente aprueba"));
            JLabel monto = new JLabel(precio(c.monto()));
            monto.setFont(monto.getFont().deriveFont(Font.BOLD, 22f));
            agregar(panel, monto);
            agregar(panel, new JLabel("Contenido del QR"));
            agregar(panel, texto(c.codigoQr()));
            agregar(panel, new JLabel("Link de pago"));
            agregar(panel, texto(c.urlPago()));
            JButton confirmar = new JButton("El cliente pagó · confirmar");
            confirmar.addActionListener(e -> {
                confirmar.setEnabled(false);
                // Qué se está pagando sale del checkout, no de quien confirma.
                Tarea.ejecutar(this, () -> api.confirmarCheckout(c.id()), this::cobrado, error -> {
                    confirmar.setEnabled(true);
                    if (!error.esSesionVencida()) Tarea.mostrarError(this, error);
                });
            });
            agregar(panel, confirmar);
            agregar(panel, Componentes.nota("El monto <b>ya tiene el descuento aplicado</b>: es el importe que se "
                    + "aprueba. La pasarela es una emulación —el host no existe—, así que el aviso de «pagó» lo da "
                    + "esta pantalla; en una integración de verdad lo dispara el procesador."));
            checkout.removeAll();
            checkout.add(Componentes.conBorde(panel));
            checkout.revalidate();
            checkout.repaint();
        }

        private JTextArea texto(String valor) {
            JTextArea area = new JTextArea(valor == null ? "" : valor);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            return area;
        }
    }

    private static void agregar(JPanel panel, JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(componente);
    }
}
