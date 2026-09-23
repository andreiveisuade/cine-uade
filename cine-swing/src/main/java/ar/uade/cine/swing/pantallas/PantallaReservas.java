package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.api.dto.Entrada;
import ar.uade.cine.swing.api.dto.Reserva;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/** El listado del encargado: se busca, se cobra y se cancela. Cobrar abre su propia pantalla. */
final class PantallaReservas extends Pantalla {

    // En el orden en que le importan a quien atiende: primero lo que hay que cobrar hoy.
    private static final List<String> ESTADOS = List.of("RESERVADA", "PAGADA", "EXPIRADA", "CANCELADA");

    private record Resultado(List<Reserva> todas, List<Reserva> visibles) {
    }

    private final Navegacion navegacion;
    private final JTextField buscar = new JTextField(22);
    private final JComboBox<Opcion<String>> estado = new JComboBox<>();
    private final JDateChooser diaFuncion = Fechas.selector(null);
    private final JLabel resumen = new JLabel(" ");
    private final JLabel conteo = new JLabel(" ");
    private final JButton cobrar = new JButton("Cobrar");
    private final JButton cancelar = new JButton("Cancelar reserva");
    private final Tabla<Reserva> tabla = new Tabla<>(
            Columna.<Reserva>de("#", r -> r.id()).ancho(40),
            Columna.<Reserva>de("Función", PantallaReservas::funcion).ancho(280),
            Columna.<Reserva>de("Cliente", r -> r.cliente() == null ? "—"
                    : r.cliente().nombre() + " · " + r.cliente().email()).ancho(240),
            Columna.<Reserva>de("Butacas", r -> r.entradas().stream().map(Entrada::codigo)
                    .collect(Collectors.joining(", "))),
            Columna.<Reserva>numero("Total", r -> precio(r.total())),
            Columna.<Reserva>de("Estado", PantallaReservas::estado).ancho(170));
    private boolean limpiando;

    PantallaReservas(ApiHttp api, Navegacion navegacion) {
        super(api, "Reservas", null);
        this.navegacion = navegacion;

        estado.addItem(new Opcion<>(null, "Todos"));
        Opcion.de(ESTADOS, v -> etiqueta(v)).forEach(estado::addItem);
        buscar.setToolTipText("cliente, email, película o butaca");

        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Buscar"));
        barra.add(buscar);
        barra.add(new JLabel("Estado"));
        barra.add(estado);
        barra.add(new JLabel("Función del día"));
        barra.add(diaFuncion);
        JButton limpiar = new JButton("Limpiar");
        barra.add(limpiar);
        barra.add(conteo);

        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.add(resumen, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(cobrar);
        acciones.add(cancelar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(norte, BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(acciones, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        // Con espera, porque cada tecla sería un pedido; un combo es una decisión, no un tanteo.
        Timer espera = Campos.alDejarDeTipear(buscar, this::buscar);
        estado.addActionListener(e -> buscar());
        diaFuncion.addPropertyChangeListener("date", e -> buscar());
        limpiar.addActionListener(e -> {
            limpiando = true;
            buscar.setText("");
            estado.setSelectedIndex(0);
            diaFuncion.setDate(null);
            limpiando = false;
            espera.stop();
            buscar();
        });

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        tabla.alDobleClic(r -> {
            if ("RESERVADA".equals(r.estado())) abrirCobro(r);
        });
        cobrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirCobro));
        cancelar.addActionListener(e -> tabla.seleccionada().ifPresent(this::cancelar));
        habilitar();
        buscar();
    }

    private static String funcion(Reserva r) {
        String titulo = r.pelicula() == null ? "—" : r.pelicula().titulo();
        if (r.funcion() == null) return titulo;
        return titulo + " · " + dia(r.funcion().inicio()) + " " + hora(r.funcion().inicio())
                + (r.sala() == null ? "" : " · " + r.sala().nombre());
    }

    private static String estado(Reserva r) {
        String texto = etiqueta(r.estado());
        if (r.pago() != null) texto += " · " + etiqueta(r.pago().medio()) + " " + hora(r.pago().fecha());
        return texto;
    }

    // Cobrar y cancelar solo tienen sentido sobre una reserva todavía sin pagar.
    private void habilitar() {
        boolean reservada = tabla.seleccionada().map(r -> "RESERVADA".equals(r.estado())).orElse(false);
        cobrar.setEnabled(reservada);
        cancelar.setEnabled(reservada);
    }

    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("q", buscar.getText());
        filtros.put("estado", Campos.elegido(estado));
        filtros.put("dia", Fechas.iso(diaFuncion));
        return filtros;
    }

    private void buscar() {
        if (limpiando) return;
        Map<String, String> filtros = filtros();
        cargar(() -> new Resultado(api.obtenerReservas(null), api.obtenerReservas(filtros)), r -> {
            List<Reserva> aCobrar = r.todas().stream().filter(x -> "RESERVADA".equals(x.estado())).toList();
            long activas = r.todas().stream().filter(x -> !"CANCELADA".equals(x.estado())).count();
            double pendiente = aCobrar.stream().mapToDouble(Reserva::total).sum();
            resumen.setText(r.todas().size() + " reservas · " + activas + " activas · " + aCobrar.size()
                    + " pendientes de cobro" + (aCobrar.isEmpty() ? "" : " (" + precio(pendiente) + ")"));
            conteo.setText(r.visibles().size() == r.todas().size() ? ""
                    : "mostrando " + r.visibles().size() + " de " + r.todas().size());
            tabla.mostrar(r.visibles());
            habilitar();
        });
    }

    private void abrirCobro(Reserva reserva) {
        navegacion.abrir(new PantallaCobro(api, navegacion, reserva.id()));
    }

    private void cancelar(Reserva reserva) {
        if (!confirmar("¿Cancelar la reserva #" + reserva.id() + "? Las butacas quedan libres.")) return;
        accion(() -> api.cancelarReserva(reserva.id()), "Reserva cancelada, las butacas quedaron libres",
                this::buscar);
    }
}
