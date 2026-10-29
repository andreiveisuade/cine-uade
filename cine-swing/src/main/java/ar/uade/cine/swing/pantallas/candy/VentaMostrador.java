package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.dto.candy.PedidoVenta;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// La pestaña de venta de mostrador: productos, medio de pago y cliente; el total lo calcula el backend.
/** El total sale de los precios de la carta: acá no se tipea. */
final class VentaMostrador extends Seccion {

    private record Carga(List<Producto> productos, List<MedioPago> medios) {
    }

    private final ApiCandy apiCandy;
    private final ApiClientes apiClientes;
    private final ProductosALaVenta productos = new ProductosALaVenta();
    private final TicketVenta ticket = new TicketVenta();
    private final JComboBox<Opcion<MedioPago>> medio = new JComboBox<>();
    private final JTextField codigo = new JTextField();
    private final JLabel etiquetaCodigo = new JLabel("Código de autorización");
    private final JTextField email = new JTextField();
    private final JTextField reserva = Campos.soloEntero(new JTextField());
    private final JLabel error = Componentes.texto(" ");

    VentaMostrador(ApiCandy apiCandy, ApiCatalogos apiCatalogos, ApiClientes apiClientes) {
        super(new BorderLayout(Escala.BLOQUE, 0));
        this.apiCandy = apiCandy;
        this.apiClientes = apiClientes;
        JButton cobrar = Componentes.primario(new JButton("Cobrar"));
        cobrar.addActionListener(e -> cobrar());
        medio.addActionListener(e -> refrescarCodigo());
        codigo.setToolTipText("El que da el posnet o la app (R11). En efectivo no hace falta.");
        reserva.setToolTipText("Con reserva, el cliente sale de ella y la venta suma al informe de esa función.");

        // Lo que se elige a la izquierda; el cobro y su ticket al costado, como el formulario de las demás pantallas.
        JPanel cobro = Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Cobro"))
                .obligatorio("Medio de pago", medio)
                .campo(etiquetaCodigo, codigo)
                .campo("Cliente (opcional)", email)
                .campo("Reserva (opcional)", reserva)
                .ancho(Componentes.nota("Con reserva, el cliente sale de ella, la venta suma al informe de esa "
                        + "función y el candy se retira en la Puerta con el QR de la entrada."))
                .ancho(Componentes.aLaDerecha(cobrar))
                .ancho(error));
        Pila costado = new Pila();
        costado.agregar(cobro).espacio(Escala.APARTE).agregar(Componentes.conBorde(ticket));
        add(productos, BorderLayout.CENTER);
        add(Componentes.lateral(costado), BorderLayout.EAST);

        cargar(() -> new Carga(apiCandy.obtenerProductosCandy(false), apiCatalogos.obtenerMediosPago()), carga -> {
            Campos.llenar(medio, Opciones.medios(carga.medios()));
            productos.mostrar(carga.productos());
            refrescarCodigo();
        });
    }

    private boolean requiereCodigo() {
        MedioPago elegido = Campos.elegido(medio);
        return elegido != null && elegido.requiereAutorizacion();
    }

    private void refrescarCodigo() {
        boolean pide = requiereCodigo();
        codigo.setEnabled(pide);
        etiquetaCodigo.setEnabled(pide);
    }

    private void cobrar() {
        Validacion v = new Validacion(error);
        MedioPago medioElegido = v.elegido(medio, "Medio de pago");
        String correo = v.email(email, "Cliente", false);
        Integer reservaId = v.entero(reserva, "Reserva", false);
        v.alMencionar("autorización", codigo);
        if (!v.ok()) return;
        Map<Integer, Integer> pedidas = productos.elegidas();
        // Si el código hace falta (R11) lo decide el backend: acá solo se deja de mandar donde no aplica.
        String autorizacion = requiereCodigo() ? codigo.getText().trim() : "";
        // Sin nada elegido no se pregunta: el backend lo rechaza y el motivo aparece junto al formulario.
        int unidades = pedidas.values().stream().mapToInt(Integer::intValue).sum();
        if (unidades > 0 && !confirmar("¿Cobrar " + cantidad(unidades, "producto", "productos")
                + " en " + etiqueta(medioElegido.nombre()).toLowerCase() + "? No se puede deshacer.", "Sí, cobrar")) {
            return;
        }
        Tarea.ejecutar(this, () -> {
            Integer clienteId = null;
            // Con reserva el email sobra: el backend toma el cliente de la reserva.
            if (correo != null && reservaId == null) {
                clienteId = apiClientes.buscarClientePorEmail(correo).id();
            }
            return apiCandy.venderCandy(new PedidoVenta(clienteId, reservaId, pedidas, medioElegido.nombre(),
                    autorizacion));
        }, compra -> {
            avisar("Cobrado " + precio(compra.total()));
            ticket.mostrar(compra);
            productos.ponerEnCero();
            codigo.setText("");
        }, v::mostrarError);
    }
}
