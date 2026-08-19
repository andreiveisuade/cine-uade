package ar.uade.cine.infrastructure.comprobantes.txt;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.infrastructure.comprobantes.GeneradorBordero;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.informes.Bordero;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Escribe el borderó en informes/bordero-funcion-&lt;id&gt;.txt, con el mismo formato de
 * texto que los tickets.
 *
 * <p>Va a <code>informes/</code> y no a <code>tickets/</code> a propósito: un ticket se le
 * entrega a un cliente y un borderó se le presenta a un organismo. Mezclarlos en una
 * carpeta obligaría a filtrar por nombre de archivo justo cuando hay que subir la
 * declaración de una función.
 *
 * <p>Reescribe el archivo de esa función cada vez que se emite, y eso es deliberado: el
 * borderó de una función es uno solo, y el que vale es el último —las entradas se siguen
 * vendiendo hasta que la película arranca—. Guardar una versión por emisión dejaría varias
 * declaraciones de la misma función sin nada que diga cuál es la buena.
 */
public class GeneradorBorderoTxt implements GeneradorBordero {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    public GeneradorBorderoTxt() {
        this(Path.of("informes"));
    }

    public GeneradorBorderoTxt(Path directorio) {
        this.directorio = directorio;
    }

    @Override
    public void emitir(Bordero bordero) {
        List<String> lineas = new ArrayList<>(List.of(
                LINEA,
                centrar("CINE UADE"),
                centrar("BORDERO INCAA"),
                centrar("FUNCION #" + bordero.funcionId()),
                LINEA,
                campo("Pelicula", bordero.pelicula()),
                campo("Sala", bordero.sala()),
                campo("Funcion", bordero.funcion().format(FORMATO_FECHA)),
                campo("Generado", bordero.generadoEn().format(FORMATO_FECHA)),
                LINEA,
                " Entradas vendidas por tarifa"));

        // Una linea por tarifa con entradas vendidas, no por cada valor del enum: las
        // tarifas en cero no se declaran, igual que el arqueo no lista los medios de pago
        // con los que no entro nada.
        for (Map.Entry<TipoTarifa, Bordero.TotalPorTarifa> tarifa : bordero.porTarifa().entrySet()) {
            lineas.add(String.format(" %-13s: %3d   $ %10s",
                    tarifa.getKey(), tarifa.getValue().cantidad(), tarifa.getValue().total()));
        }
        if (bordero.porTarifa().isEmpty()) {
            // Una funcion sin entradas vendidas se declara igual: cero tambien es un dato.
            lineas.add(" Sin entradas vendidas");
        }

        lineas.addAll(List.of(
                LINEA,
                campo("Espectadores", String.valueOf(bordero.espectadores())),
                campo("Recaudacion", "$ " + bordero.recaudacionBruta()),
                campo("Descuentos", "$ " + bordero.descuentos()),
                campo("Neto", "$ " + bordero.recaudacionNeta()),
                LINEA,
                centrar("Declaracion jurada"),
                LINEA));

        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve("bordero-funcion-" + bordero.funcionId() + ".txt"), lineas);
        } catch (IOException e) {
            throw new ComprobanteException(
                    "No se pudo emitir el bordero de la función " + bordero.funcionId(), e);
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
