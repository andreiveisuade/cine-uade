package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.infrastructure.comprobantes.GeneradorBordero;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.informes.Bordero;

/**
 * Escribe el borderó en informes/bordero-funcion-&lt;id&gt;.txt.
 *
 * <p>Va a <code>informes/</code> y no a <code>tickets/</code>: un ticket se le entrega a
 * un cliente y un borderó se le presenta a un organismo.
 *
 * <p>Reescribe el archivo de esa función cada vez: el borderó de una función es uno solo
 * y vale el último, porque las entradas se venden hasta que la película arranca.
 */
public class GeneradorBorderoTxt extends ComprobanteTxt implements GeneradorBordero {

    public GeneradorBorderoTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(Bordero bordero) {
        List<String> lineas = new ArrayList<>(List.of(
                linea(),
                centrar("CINE UADE"),
                centrar("BORDERO INCAA"),
                centrar("FUNCION #" + bordero.funcionId()),
                linea(),
                campo("Pelicula", bordero.pelicula()),
                campo("Sala", bordero.sala()),
                campo("Funcion", fecha(bordero.funcion())),
                campo("Generado", fecha(bordero.generadoEn())),
                linea(),
                " Entradas vendidas por tarifa"));

        // Solo las tarifas con ventas: las que están en cero no se declaran.
        for (Map.Entry<TipoTarifa, Bordero.TotalPorTarifa> tarifa : bordero.porTarifa().entrySet()) {
            lineas.add(String.format(" %-13s: %3d   $ %10s",
                    tarifa.getKey(), tarifa.getValue().cantidad(), tarifa.getValue().total()));
        }
        if (bordero.porTarifa().isEmpty()) {
            // Una función sin ventas se declara igual: cero también es un dato.
            lineas.add(" Sin entradas vendidas");
        }

        lineas.addAll(List.of(
                linea(),
                campo("Espectadores", String.valueOf(bordero.espectadores())),
                campo("Recaudacion", "$ " + bordero.recaudacionBruta()),
                campo("Descuentos", "$ " + bordero.descuentos()),
                campo("Neto", "$ " + bordero.recaudacionNeta()),
                linea(),
                centrar("Declaracion jurada"),
                linea()));

        escribir("bordero-funcion-" + bordero.funcionId() + ".txt", lineas,
                "el bordero de la función " + bordero.funcionId());
    }
}
