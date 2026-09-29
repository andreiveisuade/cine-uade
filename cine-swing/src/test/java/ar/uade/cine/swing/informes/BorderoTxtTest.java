package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.informes.Total;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// El texto esperado es el que escribía GeneradorBorderoTxt en el backend: si cambia un espacio, el archivo ya no es el mismo.
class BorderoTxtTest {

    private static final List<String> TARIFAS = List.of("GENERAL", "MENOR", "JUBILADO", "ESTUDIANTE");

    @Test
    void escribeElBorderoIgualQueElBackend() {
        // Como llega del JSON: alfabético, ESTUDIANTE antes que GENERAL.
        Map<String, Total> porTarifa = new LinkedHashMap<>();
        porTarifa.put("ESTUDIANTE", new Total(3, 7500));
        porTarifa.put("GENERAL", new Total(12, 60000));
        Bordero bordero = new Bordero(3, "Matrix", "Sala 1", "2026-08-13T20:30:00", "2026-08-13T19:05:42",
                15, 67500, 5000, 62500, porTarifa);

        String esperado = """
                ============================================
                                 CINE UADE
                               BORDERO INCAA
                                 FUNCION #3
                ============================================
                 Pelicula     : Matrix
                 Sala         : Sala 1
                 Funcion      : 13/08/2026 20:30
                 Generado     : 13/08/2026 19:05
                ============================================
                 Entradas vendidas por tarifa
                 GENERAL      :  12   $   60000.00
                 ESTUDIANTE   :   3   $    7500.00
                ============================================
                 Espectadores : 15
                 Recaudacion  : $ 67500.00
                 Descuentos   : $ 5000.00
                 Neto         : $ 62500.00
                ============================================
                             Declaracion jurada
                ============================================
                """;
        assertEquals(esperado, BorderoTxt.escribir(bordero, TARIFAS));
    }

    @Test
    void sinVentasLoDiceEnVezDeDejarElBloqueVacio() {
        Bordero bordero = new Bordero(8, "Up", "Sala 5", "2026-08-13T14:00:00", "2026-08-13T10:00:00",
                0, 0, 0, 0, Map.of());

        String texto = BorderoTxt.escribir(bordero, TARIFAS);

        assertEquals(" Sin entradas vendidas", texto.lines().toList().get(11));
        assertEquals(" Recaudacion  : $ 0.00", texto.lines().toList().get(14));
    }

    @Test
    void losPesosSeRedondeanACentavosComoDinero() {
        assertEquals("1234.50", BorderoTxt.pesos(1234.5));
        assertEquals("0.07", BorderoTxt.pesos(0.07));
        assertEquals("-0.50", BorderoTxt.pesos(-0.5));
        assertEquals("10.01", BorderoTxt.pesos(10.005));
    }

    @Test
    void elNombreSugeridoEsElDelBackend() {
        assertEquals("bordero-funcion-42.txt", BorderoTxt.nombreArchivo(42));
    }
}
