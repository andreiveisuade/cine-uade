package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.candy.PedidoVenta;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.informes.TicketCandy;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// La pestaña de venta de mostrador: cantidades, medio de pago y el ticket; el total lo calcula el backend.
/** El total sale de los precios de la carta: acá no se tipea. */
final class VentaMostrador extends Seccion {

    private record Carga(List<Producto> productos, List<MedioPago> medios) {
    }

    private final ApiCandy apiCandy;
    private final ApiClientes apiClientes;
    private final JPanel filas = new AlAnchoDelVisor(new GridBagLayout(), false);
    private final Cantidades cantidades = new Cantidades();
    private final JComboBox<Opcion<MedioPago>> medio = new JComboBox<>();
    private final JTextField codigo = new JTextField();
    private final JLabel etiquetaCodigo = new JLabel("Código de autorización");
    private final JTextField email = new JTextField();
    private final JTextField reserva = Campos.soloEntero(new JTextField());
    private final JLabel error = Componentes.texto(" ");
    // Cuarenta columnas: lo justo para el renglón del ticket, que mide TicketCandy.
    private final JTextArea ticket = Componentes.areaDeLectura(18, 40);
    private final JLabel tituloTicket = new JLabel(" ");

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
        JScrollPane scroll = new JScrollPane(filas);
        izquierda.add(scroll, BorderLayout.CENTER);
        izquierda.add(pie, BorderLayout.SOUTH);
        add(izquierda, BorderLayout.CENTER);

        ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ticket.setText("El total lo calcula el backend con los precios de la carta: acá no se tipea.\n"
                + "Al cobrar aparece el ticket.");
        JPanel derecha = new JPanel(new BorderLayout(0, 6));
        derecha.add(tituloTicket, BorderLayout.NORTH);
        derecha.add(new JScrollPane(ticket), BorderLayout.CENTER);
        add(derecha, BorderLayout.EAST);

        cargar(() -> new Carga(apiCandy.obtenerProductosCandy(false), apiCatalogos.obtenerMediosPago()), carga -> {
            Campos.llenar(medio, Opciones.medios(carga.medios()));
            pintar(carga.productos());
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

    private void pintar(List<Producto> productos) {
        filas.removeAll();
        cantidades.olvidar();
        if (productos.isEmpty()) {
            filas.add(Componentes.nota("No hay nada a la venta. Cargá productos en la carta."));
        }
        int fila = 0;
        for (Producto p : productos) {
            JLabel nombre = new JLabel(p.nombre());
            nombre.setFont(nombre.getFont().deriveFont(Font.BOLD));
            JPanel producto = new JPanel(new BorderLayout());
            producto.add(nombre, BorderLayout.NORTH);
            if (p.esCombo()) {
                // Debajo del nombre y sin ancho propio: un combo largo se corta con "…" en vez de empujar el
                // precio y la cantidad fuera de la vista.
                JLabel trae = new JLabel(PantallaCandy.componentesDe(p));
                trae.setForeground(Colores.secundario());
                trae.setToolTipText(PantallaCandy.componentesDe(p));
                trae.setPreferredSize(new Dimension(0, trae.getPreferredSize().height));
                producto.add(trae, BorderLayout.CENTER);
            }
            JLabel valor = new JLabel(precio(p.precio()));
            JSpinner spinner = cantidades.nueva(p.id());
            Component[] celdas = {producto, valor, spinner};
            for (int columna = 0; columna < celdas.length; columna++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = columna;
                c.gridy = fila;
                c.weightx = columna == 0 ? 1 : 0;
                c.fill = columna == 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
                c.anchor = columna >= 1 ? GridBagConstraints.EAST : GridBagConstraints.WEST;
                c.insets = new Insets(4, 8, 4, 8);
                filas.add(celdas[columna], c);
            }
            fila++;
        }
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = fila;
        relleno.weighty = 1;
        filas.add(new JLabel(), relleno);
        filas.revalidate();
        filas.repaint();
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
        Map<Integer, Integer> pedidas = cantidades.elegidas();
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
            tituloTicket.setText("Venta #" + compra.id());
            ticket.setText(TicketCandy.escribir(compra));
            cantidades.ponerEnCero();
            codigo.setText("");
        }, v::mostrarError);
    }
}
