package ar.uade.cine.swing.informes;

import ar.uade.cine.swing.api.dto.informes.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.informes.FuncionDeclarada;
import ar.uade.cine.swing.api.dto.informes.PeliculaDeclarada;
import ar.uade.cine.swing.api.dto.informes.Total;
import ar.uade.cine.swing.api.dto.informes.TotalDeclarado;
import ar.uade.cine.swing.comun.Formato;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * La declaración jurada semanal en CSV. Lo arma el cliente y no el backend: el backend da los números y esto es solo
 * su presentación en archivo. Registros etiquetados en la primera columna, cada bloque con su encabezado, y una
 * columna de entradas por cada tarifa del catálogo, vendiera o no, para que el archivo tenga siempre la misma forma.
 */
public final class DeclaracionJuradaCsv {

    static final String TEXTO_DECLARACION = "Declaro bajo juramento que los datos consignados son correctos y "
            + "completos y que corresponden a las entradas efectivamente cobradas en las funciones exhibidas en el "
            + "período.";
    // BOM: sin él, Excel en español abre el UTF-8 como Latin-1 y rompe las tildes.
    private static final String BOM = "﻿";
    private static final String FIN = "\r\n";
    private static final String SEPARADOR = ";";

    private DeclaracionJuradaCsv() {
    }

    public static String nombreArchivo(DeclaracionJurada declaracion) {
        return "declaracion-jurada-" + declaracion.desde() + "-" + declaracion.hasta() + ".csv";
    }

    /** {@code tarifas}: los nombres de {@code GET /api/tarifas}, en ese orden. */
    public static String escribir(DeclaracionJurada declaracion, List<String> tarifas) {
        List<String> entradas = tarifas.stream().map(t -> "entradas_" + t.toLowerCase()).toList();
        List<String> cierre = List.of("espectadores", "recaudacion_bruta", "descuentos", "recaudacion_neta");
        StringBuilder csv = new StringBuilder(BOM);

        fila(csv, List.of("registro", "razon_social", "cuit", "numero_exhibidor", "desde", "hasta", "generada_en"));
        fila(csv, List.of("EXHIBIDOR", declaracion.exhibidor().razonSocial(), declaracion.exhibidor().cuit(),
                declaracion.exhibidor().numeroExhibidor(), declaracion.desde(), declaracion.hasta(),
                declaracion.generadaEn()));

        fila(csv, concatenar(List.of("registro", "funcion_id", "fecha", "hora", "sala", "pelicula", "clasificacion",
                "version", "proyeccion"), entradas, cierre));
        for (FuncionDeclarada f : declaracion.funciones()) {
            fila(csv, concatenar(List.of("FUNCION", String.valueOf(f.funcionId()), LocalDateTime.parse(f.inicio()).toLocalDate().toString(),
                            Formato.hora(f.inicio()), f.sala(), f.pelicula(), f.clasificacion(), f.idioma(),
                            f.proyeccion()),
                    tarifas.stream().map(t -> cantidad(f.porTarifa(), t)).toList(),
                    montos(f.espectadores(), f.recaudacionBruta(), f.descuentos(), f.recaudacionNeta())));
        }

        fila(csv, concatenar(List.of("registro", "pelicula", "clasificacion", "funciones"), entradas, cierre));
        for (PeliculaDeclarada p : declaracion.peliculas()) {
            fila(csv, concatenar(List.of("PELICULA", p.titulo(), p.clasificacion(), String.valueOf(p.funciones())),
                    tarifas.stream().map(t -> entero(p.entradasPorTarifa(), t)).toList(),
                    montos(p.espectadores(), p.recaudacionBruta(), p.descuentos(), p.recaudacionNeta())));
        }

        TotalDeclarado total = declaracion.total();
        fila(csv, concatenar(List.of("registro", "funciones"), entradas, cierre));
        fila(csv, concatenar(List.of("TOTAL", String.valueOf(total.funciones())),
                tarifas.stream().map(t -> entero(total.entradasPorTarifa(), t)).toList(),
                montos(total.espectadores(), total.recaudacionBruta(), total.descuentos(), total.recaudacionNeta())));

        fila(csv, List.of("registro", "texto"));
        fila(csv, List.of("DECLARACION", TEXTO_DECLARACION));
        return csv.toString();
    }

    @SafeVarargs
    private static List<String> concatenar(List<String>... partes) {
        return Stream.of(partes).flatMap(List::stream).toList();
    }

    private static List<String> montos(int espectadores, double bruta, double descuentos, double neta) {
        return List.of(String.valueOf(espectadores), monto(bruta), monto(descuentos), monto(neta));
    }

    private static String cantidad(Map<String, Total> porTarifa, String tarifa) {
        Total total = porTarifa == null ? null : porTarifa.get(tarifa);
        return String.valueOf(total == null ? 0 : total.cantidad());
    }

    private static String entero(Map<String, Integer> porTarifa, String tarifa) {
        Integer valor = porTarifa == null ? null : porTarifa.get(tarifa);
        return String.valueOf(valor == null ? 0 : valor);
    }

    /** Coma decimal y dos decimales, sin separador de miles: 7500 → "7500,00". */
    static String monto(double valor) {
        long centavos = Math.round(valor * 100);
        long pesos = Math.abs(centavos) / 100;
        long resto = Math.abs(centavos) % 100;
        return (centavos < 0 ? "-" : "") + pesos + "," + (resto < 10 ? "0" : "") + resto;
    }

    /** RFC 4180: comillas si el campo trae separador, comillas o salto de línea; las comillas se duplican. */
    static String escapar(String campo) {
        String valor = campo == null ? "" : campo;
        if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private static void fila(StringBuilder csv, List<String> campos) {
        List<String> escapados = new ArrayList<>();
        campos.forEach(c -> escapados.add(escapar(c)));
        csv.append(String.join(SEPARADOR, escapados)).append(FIN);
    }
}
