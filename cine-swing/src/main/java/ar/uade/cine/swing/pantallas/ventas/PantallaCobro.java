package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.catalogos.Tarifa;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Cobro de una reserva: el detalle de lo que se cobra a un lado, el medio y el botón al otro (PanelCobro).
/**
 * El importe no se tipea: sale del total de las butacas y el descuento lo resuelve el backend al cobrar, porque depende
 * del medio de pago.
 */
public final class PantallaCobro extends Pantalla {

    private record Datos(Reserva reserva, List<MedioPago> medios, List<Tarifa> tarifas) {
    }

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final JPanel cuerpo = new JPanel(new BorderLayout());

    public PantallaCobro(ApiCatalogos apiCatalogos, ApiVentas apiVentas, Navegacion navegacion, int reservaId) {
        super("Cobrar reserva #" + reservaId, null);
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;

        JButton volver = new JButton("← Reservas");
        volver.addActionListener(e -> navegacion.ir(Destino.RESERVAS));
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        JPanel fila = new JPanel(new BorderLayout());
        fila.add(volver, BorderLayout.WEST);
        centro.add(fila, BorderLayout.NORTH);
        centro.add(cuerpo, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // GET /api/reservas/{id} trae lo mismo embebido que el listado. Un 404 se muestra en la pantalla, no en un
        // diálogo: sin reserva no hay nada más que hacer acá.
        Tarea.ejecutar(this, () -> new Datos(apiVentas.obtenerReserva(reservaId), apiCatalogos.obtenerMediosPago(),
                apiCatalogos.obtenerTarifas()), this::pintar, error -> {
            if (!error.esSesionVencida()) mostrarNota(error.getMessage());
        });
    }

    private void mostrarNota(String texto) {
        cuerpo.removeAll();
        cuerpo.add(Componentes.nota(texto), BorderLayout.NORTH);
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    private void pintar(Datos datos) {
        Reserva reserva = datos.reserva();
        if (!reserva.cobrable()) {
            mostrarNota(yaNoSeCobra(reserva));
            return;
        }
        cuerpo.removeAll();
        JPanel columnas = new JPanel(new GridLayout(1, 2, 16, 0));
        columnas.add(new PanelCobro(apiVentas, navegacion, reserva, datos.medios()));
        columnas.add(detalle(reserva, datos.tarifas()));
        cuerpo.add(columnas, BorderLayout.CENTER);
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    private static String yaNoSeCobra(Reserva reserva) {
        // Cancelable y no cobrable es una reserva que sigue abierta pero ya se pasó de hora (R17 o R19).
        String texto = "La reserva " + reserva.id() + " no se puede cobrar: "
                + (reserva.cancelable() ? "venció el plazo para pagarla o la función ya empezó."
                : "está " + etiqueta(reserva.estado()).toLowerCase() + ".");
        Pago pago = reserva.pago();
        if (pago != null) {
            texto += " Se cobró " + precio(pago.monto()) + " con " + etiqueta(pago.medio());
            if (pago.descuento() > 0) {
                texto += " (subtotal " + precio(pago.subtotal()) + " − " + precio(pago.descuento()) + " de promoción)";
            }
            texto += " el " + fechaHora(pago.fecha()) + ".";
        }
        return texto;
    }

    private JPanel detalle(Reserva reserva, List<Tarifa> tarifas) {
        // Qué tarifa se acredita lo dice el catálogo, no el nombre: una tarifa nueva no obliga a tocar esta pantalla.
        Set<String> seAcreditan = tarifas.stream().filter(Tarifa::requiereAcreditacion).map(Tarifa::nombre)
                .collect(Collectors.toSet());
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Pila arriba = new Pila();
        arriba.agregar(Componentes.subtitulo(
                reserva.pelicula() == null ? "—" : reserva.pelicula().titulo()));
        if (reserva.funcion() != null) {
            arriba.agregar(Componentes.nota(dia(reserva.funcion().inicio()) + " "
                    + hora(reserva.funcion().inicio()) + " · " + reserva.sala().nombre() + " ("
                    + etiqueta(reserva.sala().tipo()) + ")"));
        }
        arriba.agregar(new JLabel(reserva.cliente() == null ? "—"
                : reserva.cliente().nombre() + "  ·  " + reserva.cliente().email()));
        panel.add(arriba, BorderLayout.NORTH);

        Tabla<Entrada> entradas = new Tabla<>(
                Columna.<Entrada>de("Butaca", Entrada::codigo),
                Columna.<Entrada>de("Tarifa", e -> seAcreditan.contains(e.tarifa())
                        ? etiqueta(e.tarifa()) + " · se acredita en la puerta" : etiqueta(e.tarifa())),
                Columna.<Entrada>numero("Precio", e -> precio(e.precio())));
        entradas.mostrar(reserva.entradas());
        panel.add(entradas.conScroll(), BorderLayout.CENTER);
        JLabel subtotal = new JLabel("Subtotal  " + precio(reserva.total()), JLabel.RIGHT);
        subtotal.setFont(subtotal.getFont().deriveFont(Font.BOLD));
        panel.add(subtotal, BorderLayout.SOUTH);
        return Componentes.conBorde(panel);
    }
}
