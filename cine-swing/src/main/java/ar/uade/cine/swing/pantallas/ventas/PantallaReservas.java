package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Opciones;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.PantallaListado;
import ar.uade.cine.swing.pantallas.acciones.BotonesDeFila;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// El listado de reservas del encargado: se busca, se cobra y se cancela; cobrar abre su propia pantalla.
public final class PantallaReservas extends PantallaListado<Reserva> {

    // En el orden en que le importan a quien atiende: primero lo que hay que cobrar hoy.
    private static final List<String> ESTADOS = List.of("RESERVADA", "PAGADA", "EXPIRADA", "CANCELADA");

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final JButton cobrar = Componentes.primario(new JButton("Cobrar"));
    private final BotonesDeFila<Reserva> botones;
    private final Tabla<Reserva> tabla = new Tabla<>(
            Columna.<Reserva>numero("#", r -> r.id()).ancho(40),
            Columna.<Reserva>de("Función", PantallaReservas::funcion).ancho(260),
            Columna.<Reserva>de("Cliente", r -> r.cliente() == null ? "—"
                    : r.cliente().nombre() + " · " + r.cliente().email()).ancho(220),
            Columna.<Reserva>de("Butacas", r -> r.entradas().stream().map(Entrada::codigo)
                    .collect(Collectors.joining(", "))),
            Columna.<Reserva>de("Candy", PantallaReservas::candy),
            // R30: el total es el del ticket de la web, entradas y candy; la Caja los sigue desglosando.
            Columna.<Reserva>numero("Total", r -> precio(r.total() + r.totalCandy())),
            Columna.<Reserva>de("Estado", r -> etiqueta(r.estado())).comoEstado(PantallaReservas::color),
            Columna.<Reserva>de("Pago", PantallaReservas::pago).ancho(130))
            .vacia("Ninguna reserva con esos filtros");

    public PantallaReservas(ApiVentas apiVentas, Navegacion navegacion) {
        super("Reservas", "Lo que pasa por boletería: se cobra, se cancela o vence sola.");
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;
        // Command: ver AccionDeFila.
        this.botones = new BotonesDeFila<>(this, tabla, this::recargar, new CancelarReserva(apiVentas));

        JTextField buscar = new JTextField(22);
        buscar.setToolTipText("cliente, email, película o butaca");
        JComboBox<Opcion<String>> estado = new JComboBox<>();
        Campos.llenarConTodas(estado, "Todos", Opciones.etiquetadas(ESTADOS));
        filtros.texto("Buscar", "q", buscar)
                .combo("Estado", "estado", estado)
                .fecha("Función del día", "dia", Fechas.selector(null))
                .limpiar(this::buscar);

        // Cobrar al final: el primario va a la derecha de su grupo, después de lo que cancela.
        JPanel acciones = Componentes.aLaDerecha();
        botones.agregarA(acciones);
        Componentes.agregarALaDerecha(acciones, cobrar);
        add(Componentes.listado(filtros, filtros.conteo(), acciones, tabla.conScroll()), BorderLayout.CENTER);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        tabla.alDobleClic(r -> {
            if (r.cobrable()) abrirCobro(r);
        });
        cobrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirCobro));
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
        List<Reserva> aCobrar = todas.stream().filter(Reserva::cobrable).toList();
        // Activa es la que todavía cuenta para la sala: ni cancelada ni vencida.
        long activas = todas.stream().filter(r -> !List.of("CANCELADA", "EXPIRADA").contains(r.estado())).count();
        // R30: el candy pedido en la web se cobra junto con la reserva.
        double pendiente = aCobrar.stream().mapToDouble(r -> r.total() + r.totalCandy()).sum();
        return cantidad(todas.size(), "reserva", "reservas") + " · " + cantidad((int) activas, "activa", "activas")
                + " · " + cantidad(aCobrar.size(), "pendiente", "pendientes") + " de cobro"
                + (aCobrar.isEmpty() ? "" : " (" + precio(pendiente) + ")");
    }

    // El resumen se ve siempre, filtre lo que filtre: es lo que mira quien atiende la caja.
    @Override
    protected String conteo(List<Reserva> todas, List<Reserva> visibles) {
        return sinFiltrar(todas) + (visibles.size() == todas.size() ? ""
                : " · mostrando " + visibles.size() + " de " + todas.size());
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

    private static String candy(Reserva r) {
        if (r.candy().isEmpty()) return "—";
        return r.candy().stream().flatMap(c -> c.items().stream())
                .map(i -> i.cantidad() + "× " + i.nombre()).collect(Collectors.joining(", "));
    }

    // Lo que importa de cada estado: hasta cuándo se paga, con qué se pagó y si ya entró.
    private static String pago(Reserva r) {
        if ("RESERVADA".equals(r.estado()) && r.venceEn() != null) return "Vence " + hora(r.venceEn());
        if (r.pago() == null) return "—";
        String pago = etiqueta(r.pago().medio()) + " " + hora(r.pago().fecha());
        return r.ingresadaEn() == null ? pago : pago + " · entró " + hora(r.ingresadaEn());
    }

    static Color color(Reserva r) {
        return switch (r.estado()) {
            case "RESERVADA" -> Colores.aviso();
            case "PAGADA" -> Colores.exito();
            case "CANCELADA" -> Colores.error();
            default -> Colores.secundario();
        };
    }

    // Mismas reglas del backend que al cobrar y cancelar (R13, R17, R19): un botón habilitado no termina en rechazo.
    private void habilitar() {
        cobrar.setEnabled(tabla.seleccionada().map(Reserva::cobrable).orElse(false));
        botones.refrescar();
    }

    private void abrirCobro(Reserva reserva) {
        navegacion.abrirCobro(reserva.id());
    }
}
