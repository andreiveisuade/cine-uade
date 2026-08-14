package ar.uade.cine.comprobantes.txt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.comprobantes.GeneradorRecibo;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Escribe el recibo en tickets/recibo-&lt;id de pago&gt;.txt. Mismo formato y misma carpeta
 * que el ticket de la reserva: los dos se le entregan a la misma persona en el mismo
 * mostrador.
 *
 * <p>Se numera por el id del pago y no por el de la reserva porque lo que documenta es el
 * cobro. Es también lo que lo hace buscable desde el arqueo, que lista pagos.
 *
 * <p>No repite la película ni las butacas: eso ya lo dice el ticket que el cliente tiene en
 * la mano. Acá va la plata, que es lo que el ticket no puede decir —al reservar todavía no
 * se sabe el medio de pago ni, por lo tanto, qué promoción corre—.
 */
public class GeneradorReciboTxt implements GeneradorRecibo {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    public GeneradorReciboTxt() {
        this(Path.of("tickets"));
    }

    public GeneradorReciboTxt(Path directorio) {
        this.directorio = directorio;
    }

    @Override
    public void emitir(Pago pago, Reserva reserva) {
        List<String> lineas = new ArrayList<>(List.of(
                LINEA,
                centrar("CINE UADE"),
                centrar("RECIBO DE CAJA #" + pago.getId()),
                LINEA,
                campo("Reserva", "#" + reserva.getId() + " - " + reserva.getCodigo()),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Cobrado", pago.getFecha().format(FORMATO_FECHA)),
                LINEA,
                campo("Subtotal", String.format("$ %.2f", pago.getSubtotal()))));

        // El descuento solo se imprime si lo hubo: una linea "Descuento: $ 0.00" invita a
        // preguntar en la caja por que no se aplico ninguna promocion.
        if (pago.getDescuento() > 0) {
            lineas.add(campo("Descuento", String.format("$ %.2f", pago.getDescuento())));
        }

        lineas.addAll(List.of(
                campo("Total", String.format("$ %.2f", pago.getMonto())),
                campo("Pago", pago.getMedio().name()),
                LINEA,
                centrar("Comprobante de pago en efectivo"),
                centrar("Conservar hasta el ingreso a la sala"),
                LINEA));

        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve("recibo-" + pago.getId() + ".txt"), lineas);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo emitir el recibo del pago " + pago.getId(), e);
        }
    }

    private String campo(String etiqueta, String valor) {
        return String.format(" %-13s: %s", etiqueta, valor);
    }

    private String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }
}
