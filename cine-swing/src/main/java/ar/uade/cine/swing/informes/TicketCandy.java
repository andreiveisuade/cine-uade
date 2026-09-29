package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.candy.ItemCompra;
import ar.uade.cine.swing.comun.Etiquetas;
import ar.uade.cine.swing.comun.Formato;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El ticket de una venta de candy en texto de ancho fijo, como sale de una impresora de mostrador. Sin Swing, para
 * probarlo solo: la pantalla de Candy nada más lo muestra.
 */
public final class TicketCandy {

    private static final String LINEA = "=".repeat(40);
    // Lo que entra a la izquierda sin pisar el importe: un nombre más largo se corta.
    private static final int ANCHO_DETALLE = 26;

    private TicketCandy() {
    }

    public static String escribir(CompraCandy compra) {
        List<String> lineas = new ArrayList<>();
        lineas.add(LINEA);
        lineas.add("  CINE UADE · CANDY");
        lineas.add("  " + Formato.fechaHora(compra.fecha()));
        lineas.add(LINEA);
        for (ItemCompra i : compra.items()) {
            String izquierda = i.cantidad() + "x " + i.nombre();
            lineas.add(renglon(izquierda.length() > ANCHO_DETALLE ? izquierda.substring(0, ANCHO_DETALLE) : izquierda,
                    exacto(i.subtotal())));
        }
        lineas.add(LINEA);
        lineas.add(renglon("TOTAL", exacto(compra.total())));
        if (compra.ahorro() > 0) lineas.add(renglon("Ahorro por combos", exacto(compra.ahorro())));
        lineas.add(renglon("Medio", Etiquetas.etiqueta(compra.medio())));
        if (compra.codigoAutorizacion() != null && !compra.codigoAutorizacion().isEmpty()) {
            lineas.add(renglon("Autorizacion", compra.codigoAutorizacion()));
        }
        if (compra.reservaId() != null) lineas.add(renglon("Reserva", "#" + compra.reservaId()));
        lineas.add(LINEA);
        return String.join("\n", lineas);
    }

    private static String renglon(String izquierda, String derecha) {
        return " " + String.format("%-" + ANCHO_DETALLE + "s", izquierda) + String.format("%12s", derecha);
    }

    private static String exacto(double monto) {
        return "$ " + String.format(Locale.ROOT, "%.2f", monto);
    }
}
