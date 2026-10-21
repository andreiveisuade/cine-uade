package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

// Recibo de caja en un .txt por pago, con el descuento si hubo; implementa el puerto GeneradorRecibo.
// Template Method: llena los pasos de ComprobanteTxt.
public class GeneradorReciboTxt extends ComprobanteTxt<GeneradorReciboTxt.Cobro> implements GeneradorRecibo {

    // El puerto recibe dos datos y la plantilla imprime uno: este record los junta.
    record Cobro(Pago pago, Reserva reserva) {
    }

    public GeneradorReciboTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(Pago pago, Reserva reserva) {
        imprimir(new Cobro(pago, reserva));
    }

    @Override
    protected String titulo(Cobro cobro) {
        return "RECIBO DE CAJA #" + cobro.pago().getId();
    }

    @Override
    protected List<String> cuerpo(Cobro cobro) {
        Pago pago = cobro.pago();
        Reserva reserva = cobro.reserva();
        List<String> lineas = new ArrayList<>(List.of(
                campo("Reserva", "#" + reserva.getId() + " - " + reserva.getCodigo()),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Cobrado", fecha(pago.getFecha())),
                linea(),
                campo("Subtotal", "$ " + pago.getSubtotal())));

        if (pago.getDescuento().esMayorQue(Dinero.CERO)) {
            lineas.add(campo("Descuento", "$ " + pago.getDescuento()));
        }

        lineas.addAll(List.of(
                campo("Total", "$ " + pago.getMonto()),
                campo("Pago", EtiquetasTxt.de(pago.getMedio()))));
        return lineas;
    }

    @Override
    protected List<String> leyendas() {
        return List.of("Comprobante de pago en efectivo", "Conservar hasta el ingreso a la sala");
    }

    @Override
    protected String archivo(Cobro cobro) {
        return "recibo-" + cobro.pago().getId() + ".txt";
    }

    @Override
    protected String queEs(Cobro cobro) {
        return "el recibo del pago " + cobro.pago().getId();
    }
}
