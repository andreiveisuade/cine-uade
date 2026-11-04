package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.api.dto.ventas.TransaccionesCliente;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.TablaCompras;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.PantallaListado;
import ar.uade.cine.swing.pantallas.ventas.PantallaReservas;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Todos los clientes y, al elegir uno, lo que compró: sus reservas y su candy en una sola lista, lo más nuevo primero.
public final class PantallaClientes extends PantallaListado<Cliente> {

    private static final String SIN_ELEGIR = "Elegí un cliente para ver sus transacciones";

    /** Una fila de transacciones: una reserva o una venta de candy, ya con lo que se muestra. */
    private record Transaccion(String tipo, String fecha, String detalle, String butacas, String candy,
                               String estado, Color color, double total) {

        static Transaccion de(Reserva r) {
            return new Transaccion("Reserva", r.creadaEn(), PantallaReservas.funcion(r),
                    r.entradas().stream().map(Entrada::codigo).collect(Collectors.joining(", ")),
                    PantallaReservas.candy(r), etiqueta(r.estado()), PantallaReservas.color(r), r.totalConCandy());
        }

        static Transaccion de(CompraCandy c) {
            return new Transaccion("Candy", c.fecha(), "—", "—",
                    c.items().stream().map(i -> i.cantidad() + "× " + i.nombre()).collect(Collectors.joining(", ")),
                    c.situacion().texto(), TablaCompras.color(c), c.total());
        }
    }

    private final ApiClientes apiClientes;
    private final Tabla<Cliente> tabla = new Tabla<>(
            Columna.<Cliente>de("Nombre", Cliente::nombre).ancho(260),
            Columna.<Cliente>de("Email", Cliente::email).ancho(320))
            .vacia("Ningún cliente con ese nombre o email");
    private final Tabla<Transaccion> transacciones = new Tabla<>(
            Columna.<Transaccion>de("Fecha", t -> dia(t.fecha()) + " " + hora(t.fecha())).ancho(110),
            Columna.<Transaccion>de("Tipo", Transaccion::tipo).ancho(70),
            Columna.<Transaccion>de("Película o función", Transaccion::detalle).ancho(260),
            Columna.<Transaccion>de("Butacas", Transaccion::butacas),
            Columna.<Transaccion>de("Candy", Transaccion::candy),
            Columna.<Transaccion>de("Estado", Transaccion::estado).comoEstado(Transaccion::color),
            Columna.<Transaccion>numero("Total", t -> precio(t.total())))
            .vacia(SIN_ELEGIR);
    private final JLabel deQuien = Componentes.texto(" ");

    public PantallaClientes(ApiClientes apiClientes) {
        super("Clientes", "Entran a la web con su cuenta de Google: no tienen clave del cine.");
        this.apiClientes = apiClientes;

        JTextField buscar = new JTextField(22);
        buscar.setToolTipText("nombre o email");
        filtros.texto("Buscar", "q", buscar).limpiar(this::buscar);

        JPanel abajo = new JPanel(new BorderLayout(0, Escala.JUNTO));
        abajo.add(Componentes.subtitulo("Transacciones"), BorderLayout.NORTH);
        abajo.add(transacciones.conScroll(), BorderLayout.CENTER);
        abajo.add(deQuien, BorderLayout.SOUTH);
        JSplitPane partido = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Componentes.listado(filtros, filtros.conteo(), Componentes.aLaDerecha(), tabla.conScroll()), abajo);
        partido.setResizeWeight(0.5);
        partido.setBorder(null);
        add(partido, BorderLayout.CENTER);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) verTransacciones();
        });
        recargar();
    }

    @Override
    protected List<Cliente> obtener(Map<String, String> filtros) {
        return apiClientes.obtenerClientes(filtros);
    }

    @Override
    protected Tabla<Cliente> tabla() {
        return tabla;
    }

    @Override
    protected String sinFiltrar(List<Cliente> todas) {
        return cantidad(todas.size(), "cliente", "clientes");
    }

    // Se limpia antes de pedir: si el pedido falla no quedan a la vista las compras del cliente anterior.
    private void verTransacciones() {
        transacciones.vacia(SIN_ELEGIR).mostrar(List.of());
        deQuien.setText(" ");
        tabla.seleccionada().ifPresent(cliente -> Tarea.ejecutar(this,
                () -> apiClientes.obtenerTransacciones(cliente.id()),
                compradas -> mostrar(cliente, compradas),
                error -> {
                    if (sigueElegido(cliente) && !error.esSesionVencida()) deQuien.setText(error.getMessage());
                }));
    }

    // Con clics rápidos, la respuesta de un cliente puede llegar después de elegir otro.
    private boolean sigueElegido(Cliente cliente) {
        return tabla.seleccionada().filter(cliente::equals).isPresent();
    }

    private void mostrar(Cliente cliente, TransaccionesCliente compradas) {
        if (!sigueElegido(cliente)) return;
        List<Transaccion> filas = Stream.concat(
                        compradas.reservas().stream().map(Transaccion::de),
                        compradas.comprasCandy().stream().map(Transaccion::de))
                // Las fechas viajan en ISO: como texto se ordenan igual que como fecha.
                .sorted(Comparator.comparing(Transaccion::fecha).reversed())
                .toList();
        transacciones.vacia("Este cliente no tiene transacciones").mostrar(filas);
        deQuien.setText(cliente.nombre() + " · " + cliente.email() + " · "
                + cantidad(filas.size(), "transacción", "transacciones"));
    }
}
