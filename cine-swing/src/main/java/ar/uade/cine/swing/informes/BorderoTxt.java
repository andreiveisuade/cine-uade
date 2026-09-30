package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.informes.Total;
import ar.uade.cine.swing.comun.Formato;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// El borderó INCAA en texto, igual al que escribía el backend; se emite en la PC del encargado, sin Swing.
/**
 * El borderó en texto para el INCAA, idéntico al que escribía el backend (GeneradorBorderoTxt y ComprobanteTxt):
 * se emite en la PC del encargado y no en el servidor, pero el archivo no cambia. Sin Swing, para probarlo solo.
 */
public final class BorderoTxt {

    private static final String LINEA = "=".repeat(44);

    private BorderoTxt() {
    }

    public static String nombreArchivo(int funcionId) {
        return "bordero-funcion-" + funcionId + ".txt";
    }

    /**
     * {@code ordenTarifas}: el de {@code GET /api/tarifas}, que es el del enum. El backend recorría un EnumMap y el
     * JSON llega ordenado alfabético, así que sin reordenar JUBILADO saldría antes que GENERAL.
     */
    public static String escribir(Bordero bordero, List<String> ordenTarifas) {
        List<String> lineas = new ArrayList<>(List.of(
                LINEA,
                centrar("CINE UADE"),
                centrar("BORDERO INCAA"),
                centrar("FUNCION #" + bordero.funcionId()),
                LINEA,
                campo("Pelicula", bordero.pelicula()),
                campo("Sala", bordero.sala()),
                campo("Funcion", Formato.fechaHora(bordero.funcion())),
                // El `generadoEn` del backend y no el reloj de esta PC: el borderó se fecha donde se calculó.
                campo("Generado", Formato.fechaHora(bordero.generadoEn())),
                LINEA,
                " Entradas vendidas por tarifa"));

        List<Map.Entry<String, Total>> tarifas = new ArrayList<>(bordero.porTarifa().entrySet());
        tarifas.sort(Comparator.comparingInt(e -> posicion(ordenTarifas, e.getKey())));
        for (Map.Entry<String, Total> tarifa : tarifas) {
            lineas.add(String.format(Locale.ROOT, " %-13s: %3d   $ %10s",
                    tarifa.getKey(), tarifa.getValue().cantidad(), pesos(tarifa.getValue().total())));
        }
        if (tarifas.isEmpty()) {
            lineas.add(" Sin entradas vendidas");
        }

        lineas.addAll(List.of(
                LINEA,
                campo("Espectadores", String.valueOf(bordero.espectadores())),
                campo("Recaudacion", "$ " + pesos(bordero.recaudacionBruta())),
                campo("Descuentos", "$ " + pesos(bordero.descuentos())),
                campo("Neto", "$ " + pesos(bordero.recaudacionNeta())),
                LINEA,
                centrar("Declaracion jurada"),
                LINEA));

        // Files.write del backend terminaba cada línea, la última incluida, con el separador de Linux.
        StringBuilder texto = new StringBuilder();
        lineas.forEach(l -> texto.append(l).append('\n'));
        return texto.toString();
    }

    private static int posicion(List<String> orden, String tarifa) {
        int i = orden.indexOf(tarifa);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    private static String campo(String etiqueta, String valor) {
        return String.format(Locale.ROOT, " %-13s: %s", etiqueta, valor);
    }

    private static String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }

    /** Como Dinero.toString() del backend: centavos redondeados, punto decimal, sin separador de miles. */
    static String pesos(double monto) {
        long centavos = Math.round(monto * 100);
        long pesos = centavos / 100;
        long resto = Math.abs(centavos % 100);
        String signo = centavos < 0 && pesos == 0 ? "-" : "";
        return signo + pesos + "." + (resto < 10 ? "0" : "") + resto;
    }
}
