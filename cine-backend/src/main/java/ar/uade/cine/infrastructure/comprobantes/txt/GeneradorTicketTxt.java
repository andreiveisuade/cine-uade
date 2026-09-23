package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;

/** Escribe el comprobante en tickets/ticket-&lt;id&gt;.txt. */
public class GeneradorTicketTxt extends ComprobanteTxt implements GeneradorTicket {

    public GeneradorTicketTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(Reserva reserva, Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente) {
        List<String> lineas = new ArrayList<>(List.of(
                linea(),
                centrar("CINE UADE"),
                centrar("TICKET #" + reserva.getId()),
                linea(),
                campo("Pelicula", pelicula.getTitulo()),
                campo("Sala", sala.getNombre() + " (" + sala.getTipo() + ")"),
                campo("Funcion", fecha(funcion.getInicio())),
                campo("Formato", funcion.getProyeccion() + " " + funcion.getVersion()),
                campo("Cliente", cliente.getNombre()),
                campo("Emitido", fecha(reserva.getCreadaEn())),
                linea()));

        // Con la tarifa de cada butaca: es lo que se acredita en la puerta.
        for (Entrada entrada : reserva.getEntradas()) {
            String butaca = "Butaca " + entrada.codigoAsiento();
            String tarifa = entrada.tarifa() == TipoTarifa.GENERAL ? "" : " " + entrada.tarifa();
            lineas.add(String.format(" %-13s: $ %s%s", butaca, entrada.precio(), tarifa));
        }

        lineas.addAll(List.of(
                linea(),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Total", "$ " + reserva.getTotal()),
                campo("Estado", reserva.getEstado().name()),
                linea(),
                centrar("Presentar en boleteria"),
                linea()));

        escribir("ticket-" + reserva.getId() + ".txt", lineas,
                "el ticket de la reserva " + reserva.getId());
    }
}
