package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.candy.PedidoVenta;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        super(new BorderLayout(12, 8));
        this.apiCandy = apiCandy;
        this.apiClientes = apiClientes;
        JButton cobrar = new JButton("Cobrar");
        cobrar.addActionListener(e -> cobrar());
        medio.addActionListener(e -> refrescarCodigo());
        codigo.setToolTipText("El que da el posnet o la app (R11). En efectivo no hace falta.");
        reserva.setToolTipText("Con reserva, el cliente sale de ella y la venta suma al informe de esa función.");

        JPanel datos = new JPanel(new GridBagLayout());
        agregarCampo(datos, 0, new JLabel("Medio de pago *"), medio);
        agregarCampo(datos, 1, etiquetaCodigo, codigo);
        agregarCampo(datos, 2, new JLabel("Cliente (opcional)"), email);
        agregarCampo(datos, 3, new JLabel("Reserva (opcional)"), reserva);

        Pila pie = new Pila();
        pie.agregar(datos);
        pie.agregar(Componentes.nota("Con reserva, el cliente sale de ella y la venta suma al "
                + "informe de esa función."));
        pie.agregar(cobrar);
        pie.agregar(error);

        JPanel izquierda = new JPanel(new BorderLayout(0, 8));
        izquierda.add(productos, BorderLayout.CENTER);
        izquierda.add(pie, BorderLayout.SOUTH);
        add(izquierda, BorderLayout.CENTER);

        add(ticket, BorderLayout.EAST);

        cargar(() -> new Carga(apiCandy.obtenerProductosCandy(false), apiCatalogos.obtenerMediosPago()), carga -> {
            Campos.llenar(medio, Opciones.medios(carga.medios()));
            productos.mostrar(carga.productos());
            refrescarCodigo();
        });
    }

    private void agregarCampo(JPanel panel, int fila, JLabel etiqueta, Component campo) {
        GridBagConstraints izquierda = new GridBagConstraints();
        izquierda.gridy = fila;
        izquierda.anchor = GridBagConstraints.WEST;
        izquierda.insets = new Insets(3, 0, 3, 8);
        panel.add(etiqueta, izquierda);
        GridBagConstraints derecha = new GridBagConstraints();
        derecha.gridy = fila;
        derecha.gridx = 1;
        derecha.weightx = 1;
        derecha.fill = GridBagConstraints.HORIZONTAL;
        derecha.insets = new Insets(3, 0, 3, 0);
        panel.add(campo, derecha);
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
                clienteId = apiClientes.buscarClientePorEmail(correo)
                        .orElseThrow(() -> new ErrorApi(404, "No hay ningún cliente con el email " + correo)).id();
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
