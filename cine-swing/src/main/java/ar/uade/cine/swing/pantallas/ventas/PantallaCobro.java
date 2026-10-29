package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.ItemCompra;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Cobro de una reserva con su candy de la web (R30): detalle a un lado, medio y botón al otro (PanelCobro).
// El importe no se tipea: el descuento depende del medio y lo resuelve el backend al cobrar.
public final class PantallaCobro extends Pantalla {

    private record Datos(Reserva reserva, List<MedioPago> medios) {
    }

    private final ApiVentas apiVentas;
    private final Navegacion navegacion;
    private final JPanel cuerpo = new JPanel(new BorderLayout());

    public PantallaCobro(ApiCatalogos apiCatalogos, ApiVentas apiVentas, Navegacion navegacion, int reservaId) {
        super("Cobrar reserva #" + reservaId, null);
        this.apiVentas = apiVentas;
        this.navegacion = navegacion;

        JButton volver = Componentes.deTexto(new JButton("← Reservas"));
        volver.addActionListener(e -> navegacion.ir(Destino.RESERVAS));
        JPanel centro = new JPanel(new BorderLayout(0, Escala.JUNTO));
        JPanel fila = new JPanel(new BorderLayout());
        fila.add(volver, BorderLayout.WEST);
        centro.add(fila, BorderLayout.NORTH);
        centro.add(cuerpo, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // Un 404 se muestra en la pantalla y no en un diálogo: sin reserva no hay nada más que hacer.
        Tarea.ejecutar(this, () -> new Datos(apiVentas.obtenerReserva(reservaId), apiCatalogos.obtenerMediosPago()),
                this::pintar, error -> {
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
            cuerpo.removeAll();
            cuerpo.add(noSeCobra(reserva), BorderLayout.WEST);
            cuerpo.revalidate();
            cuerpo.repaint();
            return;
        }
        cuerpo.removeAll();
        JPanel columnas = new JPanel(new GridLayout(1, 2, Escala.BLOQUE, 0));
        columnas.add(new PanelCobro(apiVentas, navegacion, reserva, datos.medios()));
        columnas.add(detalle(reserva));
        cuerpo.add(columnas, BorderLayout.CENTER);
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    private static String yaNoSeCobra(Reserva reserva) {
        // Cancelable y no cobrable es una reserva que sigue abierta pero ya se pasó de hora (R17 o R19).
        String texto = "La reserva " + reserva.id() + " no se puede cobrar: "
                + (reserva.cancelable() ? "venció el plazo para pagarla o la función ya empezó"
                : "está " + etiqueta(reserva.estado()).toLowerCase());
        Pago pago = reserva.pago();
        if (pago != null) {
            texto += ". Se cobró " + precio(pago.monto()) + " con " + etiqueta(pago.medio());
            if (pago.descuento() > 0) {
                texto += " (subtotal " + precio(pago.subtotal()) + " − " + precio(pago.descuento()) + " de promoción)";
            }
            texto += " el " + fechaHora(pago.fecha());
        }
        return texto;
    }

    // Una tarjeta con lo que pasó y lo que se llevó, en vez de una línea gris en una pantalla vacía.
    private static JPanel noSeCobra(Reserva reserva) {
        Pila pila = new Pila();
        JLabel estado = Componentes.subtitulo(etiqueta(reserva.estado()));
        estado.setForeground(PantallaReservas.color(reserva));
        pila.agregar(estado);
        pila.agregar(Componentes.nota(yaNoSeCobra(reserva)));
        pila.agregar(Componentes.renglon("Entradas", precio(reserva.total()), false));
        for (CompraCandy compra : reserva.candy()) {
            pila.agregar(Componentes.renglon("Candy · " + compra.situacion().toLowerCase(), precio(compra.total()),
                    false));
        }
        pila.agregar(Componentes.renglon("Total", precio(reserva.total() + reserva.totalCandy()), true));
        JPanel tarjeta = Componentes.conBorde(pila);
        tarjeta.setPreferredSize(new Dimension(Componentes.ANCHO_LATERAL + Escala.MARGEN * 3,
                tarjeta.getPreferredSize().height));
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(tarjeta, BorderLayout.NORTH);
        return arriba;
    }

    private JPanel detalle(Reserva reserva) {
        JPanel panel = new JPanel(new BorderLayout(0, Escala.JUNTO));
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
                // Si se acredita lo dice la entrada, no el nombre: una tarifa nueva no obliga a tocar esta pantalla.
                Columna.<Entrada>de("Tarifa", e -> e.requiereAcreditacion()
                        ? etiqueta(e.tarifa()) + " · se acredita en la puerta" : etiqueta(e.tarifa())),
                Columna.<Entrada>numero("Precio", e -> precio(e.precio())));
        entradas.mostrar(reserva.entradas());
        panel.add(entradas.conScroll(), BorderLayout.CENTER);
        JLabel subtotal = new JLabel("Subtotal  " + precio(reserva.total()), JLabel.RIGHT);
        subtotal.setFont(subtotal.getFont().deriveFont(Font.BOLD));
        if (reserva.candy().isEmpty()) {
            panel.add(subtotal, BorderLayout.SOUTH);
            return Componentes.conBorde(panel);
        }
        Pila abajo = new Pila();
        abajo.agregar(subtotal);
        abajo.agregar(Componentes.subtitulo("Candy pedido en la web"));
        Tabla<ItemCompra> candy = new Tabla<>(
                Columna.<ItemCompra>de("Producto", ItemCompra::nombre),
                Columna.<ItemCompra>numero("Cantidad", i -> String.valueOf(i.cantidad())),
                Columna.<ItemCompra>numero("Subtotal", i -> precio(i.subtotal())));
        candy.mostrar(reserva.candy().stream().map(CompraCandy::items).flatMap(List::stream).toList());
        abajo.agregar(candy.conScroll());
        JLabel totalCandy = new JLabel("Candy  " + precio(reserva.totalCandy()), JLabel.RIGHT);
        totalCandy.setFont(totalCandy.getFont().deriveFont(Font.BOLD));
        abajo.agregar(totalCandy);
        panel.add(abajo, BorderLayout.SOUTH);
        return Componentes.conBorde(panel);
    }
}
