package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.Exhibidor;
import ar.uade.cine.swing.api.dto.FuncionDeclarada;
import ar.uade.cine.swing.api.dto.PeliculaDeclarada;
import ar.uade.cine.swing.api.dto.Total;
import ar.uade.cine.swing.api.dto.TotalDeclarado;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclaracionJuradaCsvTest {

    private static final List<String> TARIFAS = List.of("GENERAL", "MENOR", "JUBILADO", "ESTUDIANTE");

    private static DeclaracionJurada semana(String pelicula) {
        return new DeclaracionJurada(new Exhibidor("Cine UADE S.A.", "30-71234567-1", "10452"),
                "2026-08-20", "2026-08-26", "2026-08-27T10:00:00",
                List.of(new FuncionDeclarada(1, "2026-08-20T20:00:00", "Sala 1", pelicula, "MAS_13", "SUBTITULADA",
                        "DOS_D", 2, Map.of("GENERAL", new Total(1, 5000), "JUBILADO", new Total(1, 2500)),
                        7500, 0, 7500)),
                List.of(new PeliculaDeclarada(pelicula, "MAS_13", 1, 2, Map.of("GENERAL", 1, "JUBILADO", 1),
                        7500, 0, 7500)),
                new TotalDeclarado(1, 2, Map.of("GENERAL", 1, "JUBILADO", 1), 7500, 0, 7500));
    }

    @Test
    void escribeLosCincoBloquesConUnaColumnaPorTarifa() {
        String csv = DeclaracionJuradaCsv.escribir(semana("Matrix"), TARIFAS);

        String esperado = "﻿"
                + "registro;razon_social;cuit;numero_exhibidor;desde;hasta;generada_en\r\n"
                + "EXHIBIDOR;Cine UADE S.A.;30-71234567-1;10452;2026-08-20;2026-08-26;2026-08-27T10:00:00\r\n"
                + "registro;funcion_id;fecha;hora;sala;pelicula;clasificacion;version;proyeccion;entradas_general;"
                + "entradas_menor;entradas_jubilado;entradas_estudiante;espectadores;recaudacion_bruta;descuentos;"
                + "recaudacion_neta\r\n"
                + "FUNCION;1;2026-08-20;20:00;Sala 1;Matrix;MAS_13;SUBTITULADA;DOS_D;1;0;1;0;2;7500,00;0,00;7500,00\r\n"
                + "registro;pelicula;clasificacion;funciones;entradas_general;entradas_menor;entradas_jubilado;"
                + "entradas_estudiante;espectadores;recaudacion_bruta;descuentos;recaudacion_neta\r\n"
                + "PELICULA;Matrix;MAS_13;1;1;0;1;0;2;7500,00;0,00;7500,00\r\n"
                + "registro;funciones;entradas_general;entradas_menor;entradas_jubilado;entradas_estudiante;"
                + "espectadores;recaudacion_bruta;descuentos;recaudacion_neta\r\n"
                + "TOTAL;1;1;0;1;0;2;7500,00;0,00;7500,00\r\n"
                + "registro;texto\r\n"
                + "DECLARACION;Declaro bajo juramento que los datos consignados son correctos y completos y que "
                + "corresponden a las entradas efectivamente cobradas en las funciones exhibidas en el período.\r\n";
        assertEquals(esperado, csv);
    }

    @Test
    void lasColumnasDeEntradasSiguenElCatalogoAunqueNoHayaVenta() {
        String csv = DeclaracionJuradaCsv.escribir(semana("Matrix"), List.of("ESTUDIANTE", "GENERAL"));

        assertTrue(csv.contains("proyeccion;entradas_estudiante;entradas_general;espectadores"));
        assertTrue(csv.contains("DOS_D;0;1;2;7500,00"));
    }

    @Test
    void unTituloConSeparadorOComillasVaEntreComillas() {
        String csv = DeclaracionJuradaCsv.escribir(semana("Sí; \"otra\" vez"), TARIFAS);

        assertTrue(csv.contains(";\"Sí; \"\"otra\"\" vez\";MAS_13;"));
    }

    @Test
    void escapaSoloLoQueHaceFalta() {
        assertEquals("Matrix", DeclaracionJuradaCsv.escapar("Matrix"));
        assertEquals("\"a;b\"", DeclaracionJuradaCsv.escapar("a;b"));
        assertEquals("\"dos\nlíneas\"", DeclaracionJuradaCsv.escapar("dos\nlíneas"));
        assertEquals("\"con \"\"comillas\"\"\"", DeclaracionJuradaCsv.escapar("con \"comillas\""));
        assertEquals("", DeclaracionJuradaCsv.escapar(null));
    }

    @Test
    void losMontosVanConComaYDosDecimales() {
        assertEquals("7500,00", DeclaracionJuradaCsv.monto(7500));
        assertEquals("1234,57", DeclaracionJuradaCsv.monto(1234.567));
        assertEquals("0,05", DeclaracionJuradaCsv.monto(0.05));
        assertEquals("-12,30", DeclaracionJuradaCsv.monto(-12.3));
    }

    @Test
    void elNombreSugeridoLlevaLaSemana() {
        assertEquals("declaracion-jurada-2026-08-20-2026-08-26.csv",
                DeclaracionJuradaCsv.nombreArchivo(semana("Matrix")));
    }
}
