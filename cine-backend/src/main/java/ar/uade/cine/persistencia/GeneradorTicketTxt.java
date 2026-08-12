package ar.uade.cine.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ar.uade.cine.interfaces.Cliente;
import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.GeneradorTicket;
import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.Sala;

/**
 * Escribe el comprobante en tickets/ticket-&lt;id&gt;.txt.
 */
public class GeneradorTicketTxt implements GeneradorTicket {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    public GeneradorTicketTxt() {
        this(Path.of("tickets"));
    }

    public GeneradorTicketTxt(Path directorio) {
        this.directorio = directorio;
    }

    @Override
    public void emitir(Reserva reserva, Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente) {
        double total = funcion.getPrecio() * reserva.getCantidadEntradas();
        List<String> lineas = List.of(
                LINEA,
                centrar("CINE UADE"),
                centrar("TICKET #" + reserva.getId()),
                LINEA,
                campo("Pelicula", pelicula.getTitulo()),
                campo("Sala", sala.getNombre()),
                campo("Funcion", funcion.getInicio().format(FORMATO_FECHA)),
                campo("Cliente", cliente.getNombre()),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Precio unit.", String.format("$ %.2f", funcion.getPrecio())),
                campo("Total", String.format("$ %.2f", total)),
                campo("Estado", reserva.getEstado().name()),
                LINEA,
                centrar("Presentar en boleteria"),
                LINEA);

        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve("ticket-" + reserva.getId() + ".txt"), lineas);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo emitir el ticket de la reserva " + reserva.getId(), e);
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
