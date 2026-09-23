package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.usuarios.Cliente;

/** Escribe el comprobante en tickets/candy-&lt;id&gt;.txt. */
public class GeneradorTicketCandyTxt extends ComprobanteTxt implements GeneradorTicketCandy {

    public GeneradorTicketCandyTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(CompraCandy compra, Cliente cliente, Dinero ahorro) {
        List<String> lineas = new ArrayList<>(List.of(
                linea(),
                centrar("CINE UADE - CANDY"),
                centrar("COMPRA #" + compra.getId()),
                linea(),
                // Sin cliente es venta de mostrador.
                campo("Cliente", cliente == null ? "Consumidor final" : cliente.getNombre()),
                campo("Fecha", fecha(compra.getFecha())),
                linea()));

        for (ItemCompra item : compra.getItems()) {
            lineas.add(String.format(" %-2dx %-22s $ %8s",
                    item.cantidad(), recortar(item.nombre()), item.getSubtotal()));
        }

        lineas.add(linea());
        lineas.add(campo("Total", "$ " + compra.getTotal()));
        if (ahorro.esMayorQue(Dinero.CERO)) {
            lineas.add(campo("Ahorraste", "$ " + ahorro + " con los combos"));
        }
        lineas.add(campo("Pago", compra.getMedio().name()));
        if (!compra.getCodigoAutorizacion().isBlank()) {
            lineas.add(campo("Autorizacion", compra.getCodigoAutorizacion()));
        }
        lineas.addAll(List.of(
                linea(),
                centrar("Retirar en el candy"),
                linea()));

        escribir("candy-" + compra.getId() + ".txt", lineas,
                "el ticket de la compra " + compra.getId());
    }

    private static String recortar(String nombre) {
        return nombre.length() <= 22 ? nombre : nombre.substring(0, 21) + "…";
    }
}
