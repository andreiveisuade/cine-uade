package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.PantallaListado;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// El listado de reservas del encargado: se busca, se cobra y se cancela; cobrar abre su propia pantalla.
public final class PantallaReservas extends PantallaListado<Reserva> {

    // En el orden en que le importan a quien atiende: primero lo que hay que cobrar hoy.
    private static final List<String> ESTADOS = List.of("RESERVADA", "PAGADA", "EXPIRADA", "CANCELADA");

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final JLabel resumen = new JLabel(" ");
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

    public PantallaReservas(ApiVentas apiVentas, Navegacion navegacion) {
        super("Reservas", null);
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;

        JTextField buscar = new JTextField(22);
        buscar.setToolTipText("cliente, email, película o butaca");
        JComboBox<Opcion<String>> estado = new JComboBox<>();
        Campos.llenarConTodas(estado, "Todos", Opciones.etiquetadas(ESTADOS));
        filtros.texto("Buscar", "q", buscar)
                .combo("Estado", "estado", estado)
                .fecha("Función del día", "dia", Fechas.selector(null))
                .limpiar(this::buscar);

        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.add(resumen, BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(cobrar);
        acciones.add(cancelar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(norte, BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(acciones, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        tabla.alDobleClic(r -> {
            if (r.cobrable()) abrirCobro(r);
        });
        cobrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirCobro));
        cancelar.addActionListener(e -> tabla.seleccionada().ifPresent(this::cancelar));
        habilitar();
        recargar();
    }

    @Override
    protected List<Reserva> obtener(Map<String, String> filtros) {
        return apiVentas.obtenerReservas(filtros);
    }

    @Override
    protected Tabla<Reserva> tabla() {
        return tabla;
    }

    @Override
    protected String sinFiltrar(List<Reserva> todas) {
        return "";
    }

    @Override
    protected void alRecargar(List<Reserva> todas) {
        List<Reserva> aCobrar = todas.stream().filter(Reserva::cobrable).toList();
        long activas = todas.stream().filter(x -> !"CANCELADA".equals(x.estado())).count();
        double pendiente = aCobrar.stream().mapToDouble(Reserva::total).sum();
        resumen.setText(todas.size() + " reservas · " + activas + " activas · " + aCobrar.size()
                + " pendientes de cobro" + (aCobrar.isEmpty() ? "" : " (" + precio(pendiente) + ")"));
    }

    @Override
    protected void alMostrar(List<Reserva> visibles) {
        habilitar();
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

    // Lo decide el backend con las mismas reglas que al cobrar y cancelar (R13, R17, R19): un botón habilitado no
    // termina en un rechazo por el estado o la hora.
    private void habilitar() {
        cobrar.setEnabled(tabla.seleccionada().map(Reserva::cobrable).orElse(false));
        cancelar.setEnabled(tabla.seleccionada().map(Reserva::cancelable).orElse(false));
    }

    private void abrirCobro(Reserva reserva) {
        navegacion.abrirCobro(reserva.id());
    }

    private void cancelar(Reserva reserva) {
        if (!Mensajes.confirmar(this, "¿Cancelar la reserva #" + reserva.id() + "? Las butacas quedan libres.",
                "Sí, cancelar la reserva", "Volver")) return;
        accion(() -> apiVentas.cancelarReserva(reserva.id()), "Reserva cancelada, las butacas quedaron libres",
                this::recargar);
    }
}
