package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;

// Ticket de la reserva en un .txt, con cada butaca y su tarifa; implementa el puerto GeneradorTicket.
// Template Method: llena los pasos de ComprobanteTxt.
public class GeneradorTicketTxt extends ComprobanteTxt<Reserva> implements GeneradorTicket {

    public GeneradorTicketTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(Reserva reserva) {
        imprimir(reserva);
    }

    @Override
    protected String titulo(Reserva reserva) {
        return "TICKET #" + reserva.getId();
    }

    @Override
    protected List<String> cuerpo(Reserva reserva) {
        Funcion funcion = reserva.getFuncion();
        Sala sala = funcion.getSala();
        List<String> lineas = new ArrayList<>(List.of(
                campo("Película", funcion.getPelicula().getTitulo()),
                campo("Sala", sala.getNombre() + " (" + EtiquetasTxt.de(sala.getTipo()) + ")"),
                campo("Función", fecha(funcion.getInicio())),
                campo("Formato", EtiquetasTxt.de(funcion.getProyeccion()) + " " + EtiquetasTxt.de(funcion.getVersion())),
                campo("Cliente", reserva.getCliente().getNombre()),
                campo("Emitido", fecha(reserva.getCreadaEn())),
                linea()));

        for (Entrada entrada : reserva.getEntradas()) {
            String butaca = "Butaca " + entrada.codigoAsiento();
            String tarifa = entrada.tarifa() == TipoTarifa.GENERAL ? "" : " " + EtiquetasTxt.de(entrada.tarifa());
            lineas.add(String.format(" %-13s: $ %s%s", butaca, entrada.precio(), tarifa));
        }

        lineas.addAll(List.of(
                linea(),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Total", "$ " + reserva.getTotal()),
                campo("Estado", EtiquetasTxt.de(reserva.getEstado()))));
        return lineas;
    }

    @Override
    protected List<String> leyendas() {
        return List.of("Presentar en boletería");
    }

    @Override
    protected String archivo(Reserva reserva) {
        return "ticket-" + reserva.getId() + ".txt";
    }

    @Override
    protected String queEs(Reserva reserva) {
        return "el ticket de la reserva " + reserva.getId();
    }
}
