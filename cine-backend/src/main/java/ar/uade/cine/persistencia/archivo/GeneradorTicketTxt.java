package ar.uade.cine.persistencia.archivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.GeneradorTicket;
import ar.uade.cine.persistencia.PersistenciaException;

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
        List<String> lineas = new ArrayList<>(List.of(
                LINEA,
                centrar("CINE UADE"),
                centrar("TICKET #" + reserva.getId()),
                LINEA,
                campo("Pelicula", pelicula.getTitulo()),
                campo("Sala", sala.getNombre() + " (" + sala.getTipo() + ")"),
                campo("Funcion", funcion.getInicio().format(FORMATO_FECHA)),
                campo("Formato", funcion.getProyeccion() + " " + funcion.getVersion()),
                campo("Cliente", cliente.getNombre()),
                campo("Emitido", reserva.getCreadaEn().format(FORMATO_FECHA)),
                LINEA));

        // Detalle butaca por butaca: cada una pudo costar distinto segun su tipo y su
        // tarifa. La tarifa se imprime porque es lo que hay que acreditar en la puerta:
        // el ticket es el que dice que esa butaca se vendio como jubilado.
        for (Entrada entrada : reserva.getEntradas()) {
            String butaca = "Butaca " + entrada.codigoAsiento();
            String tarifa = entrada.tarifa() == TipoTarifa.GENERAL ? "" : " " + entrada.tarifa();
            lineas.add(String.format(" %-13s: $ %.2f%s", butaca, entrada.precio(), tarifa));
        }

        lineas.addAll(List.of(
                LINEA,
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Total", String.format("$ %.2f", reserva.getTotal())),
                campo("Estado", reserva.getEstado().name()),
                LINEA,
                centrar("Presentar en boleteria"),
                LINEA));

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
