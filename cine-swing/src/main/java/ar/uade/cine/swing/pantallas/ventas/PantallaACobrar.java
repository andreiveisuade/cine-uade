package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// La entrada de la caja: el código del ticket o el número de reserva, o una de las pendientes, y de ahí al cobro.
/**
 * Antes cobrar estaba escondido en Reservas: había que encontrar la fila entre todas las reservas, elegirla y recién
 * ahí se habilitaba el botón. Acá se llega desde el menú, con lo que trae el cliente en la mano y solo lo que falta
 * cobrar a la vista.
 */
public final class PantallaACobrar extends Pantalla {

    // El código de acceso tiene 8 caracteres; un número más corto, con o sin #, es el id de la reserva.
    private static final Pattern NUMERO = Pattern.compile("#?\\d{1,7}");

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final JTextField buscado = new JTextField(12);
    private final JLabel error = Componentes.texto(" ");
    private final JLabel resumen = new JLabel(" ");
    private final JButton cobrar = new JButton("Cobrar la seleccionada");
    private final Tabla<Reserva> pendientes = new Tabla<>(
            Columna.<Reserva>de("#", r -> r.id()).ancho(40),
            Columna.<Reserva>de("Función", PantallaACobrar::funcion).ancho(280),
            Columna.<Reserva>de("Cliente", r -> r.cliente() == null ? "—"
                    : r.cliente().nombre() + " · " + r.cliente().email()).ancho(240),
            Columna.<Reserva>de("Butacas", r -> r.entradas().stream().map(Entrada::codigo)
                    .collect(Collectors.joining(", "))),
            Columna.<Reserva>numero("Total", r -> precio(r.total())));

    public PantallaACobrar(ApiVentas apiVentas, Navegacion navegacion) {
        super("Cobrar", "Tipeá el código del ticket o el número de reserva, o elegí una de las pendientes.");
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;

        buscado.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        buscado.setToolTipText("código del ticket (8 letras y números) o número de reserva");
        JButton ir = new JButton("Ir al cobro");
        JPanel fila = new JPanel(new FlujoConSalto());
        fila.add(new JLabel("Código o # de reserva"));
        fila.add(buscado);
        fila.add(ir);
        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(fila, BorderLayout.NORTH);
        arriba.add(error, BorderLayout.SOUTH);

        JPanel listado = new JPanel(new BorderLayout(0, 8));
        listado.add(resumen, BorderLayout.NORTH);
        listado.add(pendientes.conScroll(), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(cobrar);
        listado.add(acciones, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(listado, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        buscado.addActionListener(e -> buscar());
        ir.addActionListener(e -> buscar());
        pendientes.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        pendientes.alDobleClic(r -> navegacion.abrirCobro(r.id()));
        cobrar.addActionListener(e -> pendientes.seleccionada().ifPresent(r -> navegacion.abrirCobro(r.id())));
        habilitar();
        cargarPendientes();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        buscado.requestFocusInWindow();
    }

    // RESERVADA y cobrable: una vencida puede seguir figurando RESERVADA hasta que alguien consulte su función (R17).
    private void cargarPendientes() {
        cargar(() -> apiVentas.obtenerReservas(Map.of("estado", "RESERVADA")), todas -> {
            List<Reserva> aCobrar = todas.stream().filter(Reserva::cobrable).toList();
            pendientes.mostrar(aCobrar);
            double total = aCobrar.stream().mapToDouble(Reserva::total).sum();
            resumen.setText(aCobrar.isEmpty() ? "No hay reservas pendientes de cobro."
                    : aCobrar.size() + " pendientes de cobro · " + precio(total));
            habilitar();
        });
    }

    // La pantalla de cobro ya explica por qué una reserva pagada, cancelada o vencida no se cobra: acá solo se busca.
    private void buscar() {
        Validacion v = new Validacion(error);
        String tipeado = v.texto(buscado, "Código", true);
        if (!v.ok()) return;
        String limpio = tipeado.strip();
        Tarea.ejecutar(this, () -> NUMERO.matcher(limpio).matches()
                        ? apiVentas.obtenerReserva(Integer.parseInt(limpio.replace("#", "")))
                        : apiVentas.obtenerReservaPorCodigo(limpio.toUpperCase()),
                reserva -> navegacion.abrirCobro(reserva.id()), v::mostrarError);
    }

    private void habilitar() {
        cobrar.setEnabled(pendientes.seleccionada().isPresent());
    }

    private static String funcion(Reserva r) {
        String titulo = r.pelicula() == null ? "—" : r.pelicula().titulo();
        if (r.funcion() == null) return titulo;
        return titulo + " · " + dia(r.funcion().inicio()) + " " + hora(r.funcion().inicio())
                + (r.sala() == null ? "" : " · " + r.sala().nombre());
    }
}
