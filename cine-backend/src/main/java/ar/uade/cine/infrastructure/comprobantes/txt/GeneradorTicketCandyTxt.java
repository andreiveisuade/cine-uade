package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.usuarios.Cliente;

// Ticket de candy en un .txt por compra, con lo ahorrado en combos; implementa el puerto GeneradorTicketCandy.
// Template Method: llena los pasos de ComprobanteTxt y cambia la marca del encabezado.
public class GeneradorTicketCandyTxt extends ComprobanteTxt<GeneradorTicketCandyTxt.Venta>
        implements GeneradorTicketCandy {

    // El puerto recibe dos datos y la plantilla imprime uno: este record los junta.
    record Venta(CompraCandy compra, Cliente cliente) {
    }

    public GeneradorTicketCandyTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(CompraCandy compra, Cliente cliente) {
        imprimir(new Venta(compra, cliente));
    }

    @Override
    protected String marca() {
        return "CINE UADE - CANDY";
    }

    @Override
    protected String titulo(Venta venta) {
        return "COMPRA #" + venta.compra().getId();
    }

    @Override
    protected List<String> cuerpo(Venta venta) {
        CompraCandy compra = venta.compra();
        Cliente cliente = venta.cliente();
        List<String> lineas = new ArrayList<>(List.of(
                campo("Cliente", cliente == null ? "Consumidor final" : cliente.getNombre()),
                campo("Fecha", fecha(compra.getFecha())),
                linea()));

        for (ItemCompra item : compra.getItems()) {
            lineas.add(String.format(" %-2dx %-22s $ %8s",
                    item.cantidad(), recortar(item.nombre()), item.getSubtotal()));
        }

        lineas.add(linea());
        lineas.add(campo("Total", "$ " + compra.getTotal()));
        Dinero ahorro = compra.getAhorro();
        if (ahorro.esMayorQue(Dinero.CERO)) {
            lineas.add(campo("Ahorraste", "$ " + ahorro + " con los combos"));
        }
        lineas.add(campo("Pago", EtiquetasTxt.de(compra.getMedio())));
        if (!compra.getCodigoAutorizacion().isBlank()) {
            lineas.add(campo("Autorización", compra.getCodigoAutorizacion()));
        }
        return lineas;
    }

    @Override
    protected List<String> leyendas() {
        return List.of("Retirar en el candy");
    }

    @Override
    protected String archivo(Venta venta) {
        return "candy-" + venta.compra().getId() + ".txt";
    }

    @Override
    protected String queEs(Venta venta) {
        return "el ticket de la compra " + venta.compra().getId();
    }

    private static String recortar(String nombre) {
        return nombre.length() <= 22 ? nombre : nombre.substring(0, 21) + "…";
    }
}
