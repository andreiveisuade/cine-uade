package ar.uade.cine.persistencia.archivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.persistencia.GeneradorTicketCandy;
import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Escribe el comprobante en tickets/candy-&lt;id&gt;.txt. Mismo medio y mismo formato que
 * el ticket de butacas, en un archivo aparte para no confundir los dos comprobantes.
 */
public class GeneradorTicketCandyTxt implements GeneradorTicketCandy {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    public GeneradorTicketCandyTxt() {
        this(Path.of("tickets"));
    }

    public GeneradorTicketCandyTxt(Path directorio) {
        this.directorio = directorio;
    }

    @Override
    public void emitir(CompraCandy compra, Cliente cliente, double ahorro) {
        List<String> lineas = new ArrayList<>(List.of(
                LINEA,
                centrar("CINE UADE - CANDY"),
                centrar("COMPRA #" + compra.getId()),
                LINEA,
                campo("Cliente", cliente.getNombre()),
                campo("Fecha", compra.getFecha().format(FORMATO_FECHA)),
                LINEA));

        for (ItemCompra item : compra.getItems()) {
            lineas.add(String.format(" %-2dx %-22s $ %8.2f",
                    item.cantidad(), recortar(item.nombre()), item.getSubtotal()));
        }

        lineas.add(LINEA);
        lineas.add(campo("Total", String.format("$ %.2f", compra.getTotal())));
        if (ahorro > 0) {
            lineas.add(campo("Ahorraste", String.format("$ %.2f con los combos", ahorro)));
        }
        lineas.add(campo("Pago", compra.getMedio().name()));
        if (!compra.getCodigoAutorizacion().isBlank()) {
            lineas.add(campo("Autorizacion", compra.getCodigoAutorizacion()));
        }
        lineas.addAll(List.of(
                LINEA,
                centrar("Retirar en el candy"),
                LINEA));

        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve("candy-" + compra.getId() + ".txt"), lineas);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo emitir el ticket de la compra " + compra.getId(), e);
        }
    }

    private String campo(String etiqueta, String valor) {
        return String.format(" %-13s: %s", etiqueta, valor);
    }

    private String recortar(String nombre) {
        return nombre.length() <= 22 ? nombre : nombre.substring(0, 21) + "…";
    }

    private String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }
}
