package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.Cliente;
import ar.uade.cine.swing.api.dto.CompraCandy;
import ar.uade.cine.swing.api.dto.ItemCompra;
import ar.uade.cine.swing.api.dto.MedioPago;
import ar.uade.cine.swing.api.dto.PedidoCombo;
import ar.uade.cine.swing.api.dto.PedidoProducto;
import ar.uade.cine.swing.api.dto.PedidoVenta;
import ar.uade.cine.swing.api.dto.Producto;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formato;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tarea;
import com.toedter.calendar.JDateChooser;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

/**
 * La otra caja del cine (CU-13 a CU-16). Pestañas y no una pantalla larga: cobrar un pochoclo no puede pedir
 * scrollear entre combos.
 */
final class PantallaCandy extends Pantalla {

    private record Carga(List<Producto> productos, List<MedioPago> medios) {
    }

    private static final List<String> TIPOS_SUELTOS = List.of("POCHOCLOS", "BEBIDA", "GOLOSINA");

    PantallaCandy(ApiHttp api, Navegacion navegacion) {
        super(api, "Candy", "La otra caja del cine: se cobra en el mostrador y se entrega, sin reserva de por medio.");
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Carta", new Carta());
        pestanas.addTab("Venta de mostrador", new Venta());
        pestanas.addTab("Ventas del día", new Ventas());
        add(pestanas, BorderLayout.CENTER);
    }

    private static String componentesDe(Producto p) {
        if (p.componentes() == null || p.componentes().isEmpty()) return "—";
        return p.componentes().stream().map(c -> c.cantidad() + "× " + c.nombre()).collect(Collectors.joining(" + "));
    }

    private static JSpinner cantidad() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        spinner.setPreferredSize(new Dimension(70, spinner.getPreferredSize().height));
        return spinner;
    }

    // Solo lo que lleva al menos una unidad: el backend recibe productoId → cantidad.
    private static Map<Integer, Integer> elegidas(Map<Integer, JSpinner> cantidades) {
        Map<Integer, Integer> elegidas = new LinkedHashMap<>();
        cantidades.forEach((id, spinner) -> {
            int n = (Integer) spinner.getValue();
            if (n > 0) elegidas.put(id, n);
        });
        return elegidas;
    }

    private static JLabel etiquetaError() {
        JLabel error = new JLabel(" ");
        error.setForeground(Colores.error());
        return error;
    }

    private static void mostrarError(JLabel etiqueta, ErrorApi error) {
        if (!error.esSesionVencida()) {
            etiqueta.setText("<html><div style='width:320px'>" + error.getMessage() + "</div></html>");
        }
    }

    /** La carta: altas, edición de nombre y precio, y sacar o reponer. Los productos no se borran. */
    private final class Carta extends JPanel {

        private final JButton editar = new JButton("Editar");
        private final JButton alternar = new JButton("Sacar de la carta");
        private final Tabla<Producto> tabla = new Tabla<>(
                Columna.<Producto>de("Producto", Producto::nombre).ancho(200),
                Columna.<Producto>de("Tipo", p -> etiqueta(p.tipo())),
                Columna.<Producto>de("Trae", PantallaCandy::componentesDe).ancho(220),
                Columna.<Producto>numero("Precio", p -> precio(p.precio())),
                Columna.<Producto>de("Estado", p -> p.disponible() ? "A la venta" : "Fuera de la carta"));

        private final JTextField nombreProducto = new JTextField();
        private final JComboBox<Opcion<String>> tipoProducto = new JComboBox<>();
        private final JTextField precioProducto = new JTextField();
        private final JLabel errorProducto = etiquetaError();

        private final JTextField nombreCombo = new JTextField();
        private final JTextField precioCombo = new JTextField();
        private final JPanel filasCombo = new JPanel(new GridBagLayout());
        private final Map<Integer, JSpinner> cantidadesCombo = new LinkedHashMap<>();
        private final JLabel errorCombo = etiquetaError();

        Carta() {
            super(new BorderLayout(12, 8));
            JPanel acciones = new JPanel(new FlujoConSalto());
            acciones.add(editar);
            acciones.add(alternar);
            JPanel abajo = new JPanel(new BorderLayout(0, 4));
            abajo.add(acciones, BorderLayout.NORTH);
            abajo.add(Componentes.nota("Los productos no se borran: se sacan de la carta. Uno que ya se vendió tiene "
                    + "que seguir existiendo para que el ticket de esa venta diga qué se llevó. Cambiar un precio no "
                    + "toca las ventas hechas: cada una guardó el precio que tenía."), BorderLayout.CENTER);

            JPanel centro = new JPanel(new BorderLayout(0, 8));
            centro.add(tabla.conScroll(), BorderLayout.CENTER);
            centro.add(abajo, BorderLayout.SOUTH);
            add(centro, BorderLayout.CENTER);
            add(altas(), BorderLayout.EAST);

            tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
            tabla.alDobleClic(this::editar);
            editar.addActionListener(e -> tabla.seleccionada().ifPresent(this::editar));
            alternar.addActionListener(e -> tabla.seleccionada().ifPresent(p ->
                    accion(() -> api.cambiarDisponibilidadCandy(p.id(), !p.disponible()), null, this::recargar)));
            habilitar();
            recargar();
        }

        private void habilitar() {
            var elegido = tabla.seleccionada();
            editar.setEnabled(elegido.isPresent());
            alternar.setEnabled(elegido.isPresent());
            alternar.setText(elegido.map(p -> p.disponible() ? "Sacar de la carta" : "Reponer")
                    .orElse("Sacar de la carta"));
        }

        private JScrollPane altas() {
            Opcion.de(TIPOS_SUELTOS, v -> etiqueta(v)).forEach(tipoProducto::addItem);
            JButton agregar = new JButton("Agregar a la carta");
            agregar.addActionListener(e -> crearProducto());
            JButton armar = new JButton("Armar combo");
            armar.addActionListener(e -> armarCombo());

            JPanel columna = new JPanel();
            columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
            columna.add(Componentes.izquierda(Componentes.conBorde(new Componentes.Formulario()
                    .ancho(Componentes.subtitulo("Nuevo producto"))
                    .campo("Nombre", nombreProducto)
                    .campo("Tipo", tipoProducto)
                    .campo("Precio", precioProducto)
                    .ancho(agregar)
                    .ancho(errorProducto))));
            columna.add(Componentes.izquierda(Componentes.conBorde(new Componentes.Formulario()
                    .ancho(Componentes.subtitulo("Armar combo"))
                    .ancho(Componentes.nota("Al menos dos productos. El combo tiene que salir menos que sus "
                            + "componentes sueltos (R14): si no, no habría motivo para ofrecerlo."))
                    .campo("Nombre", nombreCombo)
                    .ancho(new JLabel("Qué trae (cantidad)"))
                    .ancho(filasCombo)
                    .campo("Precio del combo", precioCombo)
                    .ancho(armar)
                    .ancho(errorCombo))));
            JScrollPane scroll = new JScrollPane(columna);
            scroll.setBorder(null);
            scroll.setPreferredSize(new Dimension(330, 0));
            return scroll;
        }

        private void recargar() {
            cargar(() -> api.obtenerProductosCandy(true), productos -> {
                tabla.mostrar(productos);
                pintarComponentes(productos.stream().filter(p -> !p.esCombo()).toList());
                habilitar();
            });
        }

        private void pintarComponentes(List<Producto> sueltos) {
            filasCombo.removeAll();
            cantidadesCombo.clear();
            if (sueltos.isEmpty()) {
                filasCombo.add(Componentes.nota("Primero cargá productos sueltos."));
            }
            int fila = 0;
            for (Producto p : sueltos) {
                JLabel nombre = new JLabel(p.nombre() + "  · " + precio(p.precio()));
                if (!p.disponible()) nombre.setForeground(Componentes.gris());
                JSpinner spinner = cantidad();
                cantidadesCombo.put(p.id(), spinner);
                GridBagConstraints izquierda = new GridBagConstraints();
                izquierda.gridy = fila;
                izquierda.weightx = 1;
                izquierda.anchor = GridBagConstraints.WEST;
                izquierda.insets = new Insets(2, 0, 2, 8);
                filasCombo.add(nombre, izquierda);
                GridBagConstraints derecha = new GridBagConstraints();
                derecha.gridy = fila++;
                derecha.gridx = 1;
                filasCombo.add(spinner, derecha);
            }
            filasCombo.revalidate();
            filasCombo.repaint();
        }

        private void crearProducto() {
            PedidoProducto pedido = new PedidoProducto(Campos.texto(nombreProducto), Campos.elegido(tipoProducto),
                    Campos.decimal(precioProducto));
            errorProducto.setText(" ");
            Tarea.ejecutar(this, () -> api.crearProductoCandy(pedido), creado -> {
                avisar("Producto agregado");
                nombreProducto.setText("");
                precioProducto.setText("");
                recargar();
            }, e -> mostrarError(errorProducto, e));
        }

        // El mínimo de dos productos y R14 los valida el backend: su mensaje aparece abajo del formulario.
        private void armarCombo() {
            PedidoCombo pedido = new PedidoCombo(Campos.texto(nombreCombo), Campos.decimal(precioCombo),
                    elegidas(cantidadesCombo));
            errorCombo.setText(" ");
            Tarea.ejecutar(this, () -> api.armarComboCandy(pedido), creado -> {
                avisar("Combo armado");
                nombreCombo.setText("");
                precioCombo.setText("");
                recargar();
            }, e -> mostrarError(errorCombo, e));
        }

        // Nombre y precio: lo único que el backend deja cambiar.
        private void editar(Producto p) {
            JTextField nombre = new JTextField(p.nombre(), 20);
            JTextField valor = new JTextField(p.precio() % 1 == 0 ? String.valueOf((long) p.precio())
                    : String.valueOf(p.precio()));
            Componentes.Formulario formulario = new Componentes.Formulario()
                    .campo("Nombre", nombre)
                    .campo("Precio", valor);
            if (p.esCombo()) {
                formulario.ancho(Componentes.nota("Trae " + componentesDe(p) + ". Los componentes se fijan al armarlo."));
            }
            int opcion = JOptionPane.showConfirmDialog(this, formulario, "Editar " + p.nombre(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;
            String nuevoNombre = Campos.texto(nombre);
            Double nuevoPrecio = Campos.decimal(valor);
            accion(() -> api.editarProductoCandy(p.id(), nuevoNombre, nuevoPrecio), "Producto actualizado",
                    this::recargar);
        }
    }

    /** Venta de mostrador. El total lo calcula el backend con los precios de la carta: acá no se tipea. */
    private final class Venta extends JPanel {

        private final JPanel filas = new JPanel(new GridBagLayout());
        private final Map<Integer, JSpinner> cantidades = new LinkedHashMap<>();
        private final JComboBox<Opcion<String>> medio = new JComboBox<>();
        private final JTextField codigo = new JTextField();
        private final JLabel etiquetaCodigo = new JLabel("Código de autorización");
        private final JTextField email = new JTextField();
        private final JTextField reserva = new JTextField();
        private final JLabel error = etiquetaError();
        private final JTextArea ticket = new JTextArea(18, 42);
        private final JLabel tituloTicket = new JLabel(" ");
        private List<MedioPago> medios = List.of();

        Venta() {
            super(new BorderLayout(12, 8));
            JButton cobrar = new JButton("Cobrar");
            cobrar.addActionListener(e -> cobrar());
            medio.addActionListener(e -> refrescarCodigo());
            codigo.setToolTipText("El que da el posnet o la app (R11). En efectivo no hace falta.");
            reserva.setToolTipText("Con reserva, el cliente sale de ella y la venta suma al informe de esa función.");

            JPanel datos = new JPanel(new GridBagLayout());
            agregarCampo(datos, 0, new JLabel("Medio de pago"), medio);
            agregarCampo(datos, 1, etiquetaCodigo, codigo);
            agregarCampo(datos, 2, new JLabel("Cliente (opcional)"), email);
            agregarCampo(datos, 3, new JLabel("Reserva (opcional)"), reserva);

            JPanel pie = new JPanel();
            pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
            pie.add(Componentes.izquierda(datos));
            pie.add(Componentes.izquierda(Componentes.nota("Con reserva, el cliente sale de ella y la venta suma al "
                    + "informe de esa función.")));
            pie.add(Componentes.izquierda(cobrar));
            pie.add(Componentes.izquierda(error));

            JPanel izquierda = new JPanel(new BorderLayout(0, 8));
            JScrollPane scroll = new JScrollPane(filas);
            izquierda.add(scroll, BorderLayout.CENTER);
            izquierda.add(pie, BorderLayout.SOUTH);
            add(izquierda, BorderLayout.CENTER);

            ticket.setEditable(false);
            ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            ticket.setText("El total lo calcula el backend con los precios de la carta: acá no se tipea.\n"
                    + "Al cobrar aparece el ticket.");
            JPanel derecha = new JPanel(new BorderLayout(0, 6));
            derecha.add(tituloTicket, BorderLayout.NORTH);
            derecha.add(new JScrollPane(ticket), BorderLayout.CENTER);
            add(derecha, BorderLayout.EAST);

            cargar(() -> new Carga(api.obtenerProductosCandy(false), api.obtenerMediosPago()), carga -> {
                medios = carga.medios();
                medios.forEach(m -> medio.addItem(new Opcion<>(m.nombre(), etiqueta(m.nombre()))));
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
            cantidades.clear();
            if (productos.isEmpty()) {
                filas.add(Componentes.nota("No hay nada a la venta. Cargá productos en la carta."));
            }
            int fila = 0;
            for (Producto p : productos) {
                JLabel nombre = new JLabel(p.nombre());
                nombre.setFont(nombre.getFont().deriveFont(Font.BOLD));
                JLabel trae = new JLabel(componentesDe(p));
                trae.setForeground(Componentes.gris());
                JLabel valor = new JLabel(precio(p.precio()));
                JSpinner spinner = cantidad();
                cantidades.put(p.id(), spinner);
                Component[] celdas = {nombre, trae, valor, spinner};
                for (int columna = 0; columna < celdas.length; columna++) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = columna;
                    c.gridy = fila;
                    c.weightx = columna == 1 ? 1 : 0;
                    c.anchor = columna >= 2 ? GridBagConstraints.EAST : GridBagConstraints.WEST;
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
            String elegido = Campos.elegido(medio);
            return medios.stream().anyMatch(m -> m.nombre().equals(elegido) && m.requiereAutorizacion());
        }

        private void refrescarCodigo() {
            boolean pide = requiereCodigo();
            codigo.setEnabled(pide);
            etiquetaCodigo.setEnabled(pide);
        }

        private void cobrar() {
            error.setText(" ");
            Map<Integer, Integer> pedidas = elegidas(cantidades);
            String medioElegido = Campos.elegido(medio);
            String autorizacion = requiereCodigo() ? codigo.getText().trim() : "";
            String correo = email.getText().trim();
            Integer reservaId = Campos.entero(reserva);
            String reservaTipeada = reserva.getText().trim();
            Tarea.ejecutar(this, () -> {
                Integer clienteId = null;
                // Con reserva el email sobra: el backend toma el cliente de la reserva.
                if (!correo.isEmpty() && reservaTipeada.isEmpty()) {
                    Cliente cliente = api.buscarClientePorEmail(correo);
                    if (cliente == null) throw new ErrorApi(404, "No hay ningún cliente con el email " + correo);
                    clienteId = cliente.id();
                }
                return api.venderCandy(new PedidoVenta(clienteId, reservaId, pedidas, medioElegido, autorizacion));
            }, compra -> {
                avisar("Cobrado " + precio(compra.total()));
                tituloTicket.setText("Venta #" + compra.id());
                ticket.setText(ticketDe(compra));
                cantidades.values().forEach(s -> s.setValue(0));
                codigo.setText("");
            }, e -> mostrarError(error, e));
        }
    }

    private static final String LINEA = "=".repeat(40);

    static String ticketDe(CompraCandy compra) {
        List<String> lineas = new ArrayList<>();
        lineas.add(LINEA);
        lineas.add("  CINE UADE · CANDY");
        lineas.add("  " + Formato.fechaHora(compra.fecha()));
        lineas.add(LINEA);
        for (ItemCompra i : compra.items()) {
            String izquierda = i.cantidad() + "x " + i.nombre();
            lineas.add(renglon(izquierda.length() > 26 ? izquierda.substring(0, 26) : izquierda, exacto(i.subtotal())));
        }
        lineas.add(LINEA);
        lineas.add(renglon("TOTAL", exacto(compra.total())));
        if (compra.ahorro() > 0) lineas.add(renglon("Ahorro por combos", exacto(compra.ahorro())));
        lineas.add(renglon("Medio", etiqueta(compra.medio())));
        if (compra.codigoAutorizacion() != null && !compra.codigoAutorizacion().isEmpty()) {
            lineas.add(renglon("Autorizacion", compra.codigoAutorizacion()));
        }
        if (compra.reservaId() != null) lineas.add(renglon("Reserva", "#" + compra.reservaId()));
        lineas.add(LINEA);
        return String.join("\n", lineas);
    }

    private static String renglon(String izquierda, String derecha) {
        return " " + String.format("%-26s", izquierda) + String.format("%12s", derecha);
    }

    private static String exacto(double monto) {
        return "$ " + String.format(Locale.ROOT, "%.2f", monto);
    }

    /** Las ventas de un día. El total cobrado está en Caja, al lado de la boletería. */
    private final class Ventas extends JPanel {

        private final JDateChooser fecha = Fechas.selector(LocalDate.now());
        private final JLabel cantidad = new JLabel(" ");
        private final Tabla<CompraCandy> tabla = PantallaCaja.tablaCompras();

        Ventas() {
            super(new BorderLayout(0, 8));
            cantidad.setFont(cantidad.getFont().deriveFont(Font.BOLD, 18f));
            JPanel barra = new JPanel(new FlujoConSalto());
            barra.add(new JLabel("Fecha"));
            barra.add(fecha);
            barra.add(cantidad);
            add(barra, BorderLayout.NORTH);
            add(tabla.conScroll(), BorderLayout.CENTER);
            add(Componentes.nota("El total cobrado del día está en Caja, al lado de la boletería."), BorderLayout.SOUTH);
            fecha.addPropertyChangeListener("date", e -> recargar());
            recargar();
        }

        private void recargar() {
            Map<String, String> filtros = new LinkedHashMap<>();
            filtros.put("fecha", Fechas.iso(fecha));
            cargar(() -> api.obtenerComprasCandy(filtros), compras -> {
                cantidad.setText(compras.size() + (compras.size() == 1 ? " venta" : " ventas"));
                tabla.mostrar(compras);
            });
        }
    }
}
