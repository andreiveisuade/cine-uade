package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// El lado del cobro: medio de pago y el botón que cobra en caja o abre el checkout, según pida el catálogo.
final class PanelCobro extends Seccion implements CajaDeCobro {

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final Reserva reserva;
    private final JComboBox<Opcion<MedioPago>> medio = new JComboBox<>();
    private final JLabel explicacion = Componentes.nota("");
    private final JButton enviar = Componentes.primario(new JButton());
    private final JLabel error = Componentes.texto(" ");
    private final JPanel checkout = new JPanel(new BorderLayout());
    private FormaDeCobro forma;
    private Validacion validacion;

    PanelCobro(ApiVentas apiVentas, Navegacion navegacion, Reserva reserva, List<MedioPago> medios) {
        super(new BorderLayout());
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;
        this.reserva = reserva;
        Campos.llenar(medio, Opciones.medios(medios));

        JLabel total = new JLabel(precio(reserva.total() + reserva.totalCandy()));
        total.setFont(total.getFont().deriveFont(Font.BOLD, Escala.LETRA_TITULO));

        Pila contenido = new Pila();
        contenido.agregar(Componentes.subtitulo("Cobro"));
        contenido.agregar(new JLabel("Medio de pago *"));
        medio.setMaximumSize(new Dimension(Integer.MAX_VALUE, medio.getPreferredSize().height));
        contenido.agregar(medio);
        contenido.agregar(new JLabel(" "));
        contenido.agregar(new JLabel("A cobrar"));
        contenido.agregar(total);
        contenido.agregar(Componentes.nota((reserva.candy().isEmpty() ? "Sale del total de las butacas"
                : "Las butacas y el candy pedido en la web se cobran juntos, con el mismo medio")
                + ": no se puede cobrar otro importe. Si hay una promoción vigente para este medio de pago, el "
                + "descuento se aplica al cobrar, solo a las entradas."));
        contenido.agregar(new JLabel(" "));
        contenido.agregar(explicacion);
        contenido.agregar(enviar);
        contenido.agregar(error);
        contenido.agregar(checkout);
        add(Componentes.conBorde(contenido), BorderLayout.NORTH);

        // Un checkout es de un medio y un monto concretos: cambiar el medio lo invalida.
        medio.addActionListener(e -> {
            Componentes.reemplazar(checkout);
            elegirForma();
        });
        enviar.addActionListener(e -> enviar());
        elegirForma();
    }

    // Strategy: el medio elegido decide la forma de cobro, y el botón y la explicación salen de ella.
    private void elegirForma() {
        forma = FormaDeCobro.para(Campos.elegido(medio), apiVentas);
        enviar.setText(forma.boton());
        explicacion.setText("<html>" + forma.explicacion() + "</html>");
    }

    private void enviar() {
        validacion = new Validacion(error);
        MedioPago elegido = validacion.elegido(medio, "Medio de pago");
        if (!validacion.ok()) return;
        forma.enviar(reserva, elegido, this);
    }

    @Override
    public boolean confirmar(String pregunta, String si) {
        return super.confirmar(pregunta, si);
    }

    @Override
    public <T> void pedir(Callable<T> trabajo, Consumer<T> alTerminar) {
        enviar.setEnabled(false);
        Tarea.ejecutar(this, trabajo, alTerminar, e -> {
            enviar.setEnabled(true);
            validacion.mostrarError(e);
        });
    }

    @Override
    public void cobrado(Pago pago) {
        String candy = reserva.candy().isEmpty() ? "" : " · candy " + precio(reserva.totalCandy());
        avisar(pago.descuento() > 0
                ? "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()) + " · "
                + precio(pago.descuento()) + " de descuento" + candy
                : "Cobrado " + precio(pago.monto()) + " con " + etiqueta(pago.medio()) + candy);
        navegacion.ir(Destino.CAJA);
    }

    @Override
    public void mostrarCheckout(Checkout abierto) {
        enviar.setEnabled(true);
        Componentes.reemplazar(checkout, new PanelCheckout(apiVentas, abierto, this::cobrado));
    }
}
