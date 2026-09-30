package ar.uade.cine.model.salas.validacion;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.validacion.Regla;

// Reglas de los datos de una sala y de su distribución de butacas; Fabricación pura que llama Sala.
public final class ValidadorSala {

    // El VARCHAR(50) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO_DEL_NOMBRE = 50;

    // Dos horas alcanzan para la limpieza más larga: más es una sala cerrada, no una limpieza. Sin
    // tope, 2147483647 desbordaba el int del margen con que R3 busca los choques.
    private static final int MAXIMO_MINUTOS_DE_LIMPIEZA = 120;

    // Cada fila se nombra con una letra: ver Asiento.codigoDe.
    private static final int MAXIMO_FILAS = 26;

    // Sin tope, [100000] creaba cien mil butacas en una fila. Cuarenta alcanza para la sala más ancha.
    private static final int MAXIMO_BUTACAS_POR_FILA = 40;

    private ValidadorSala() {
    }

    // Devuelve el nombre como se guarda: sin los espacios de las puntas, que harían pasar " Sala 1" por
    // otra sala al buscar repetidos.
    public static String nombre(String nombre) {
        return Regla.texto(nombre).obligatorio("Falta el nombre").recortado()
                .hasta(LARGO_MAXIMO_DEL_NOMBRE, "El nombre").valor();
    }

    public static void tipo(TipoSala tipo) {
        Regla.objeto(tipo).obligatorio("Falta el tipo de sala");
    }

    public static void limpieza(int minutos) {
        Regla.numero(minutos).noNegativo("Los minutos de limpieza no pueden ser negativos")
                .entre(0, MAXIMO_MINUTOS_DE_LIMPIEZA,
                        "La limpieza no puede durar más de " + MAXIMO_MINUTOS_DE_LIMPIEZA + " minutos");
    }

    // Primero que ninguna fila esté vacía y después que ninguna se pase: así [41, 0] dice lo de la vacía.
    public static void distribucion(List<Integer> butacasPorFila) {
        Regla.lista(butacasPorFila).noVacia("La sala tiene que tener al menos una fila")
                .hasta(MAXIMO_FILAS, "La sala tiene que tener como máximo " + MAXIMO_FILAS
                        + " filas: se identifican con una letra")
                .sinNulos("Cada fila tiene que tener al menos una butaca");
        butacasPorFila.forEach(butacas -> Regla.numero(butacas)
                .mayorQueCero("Cada fila tiene que tener al menos una butaca"));
        butacasPorFila.forEach(butacas -> Regla.numero(butacas).entre(1, MAXIMO_BUTACAS_POR_FILA,
                "Una fila tiene que tener como máximo " + MAXIMO_BUTACAS_POR_FILA + " butacas"));
    }

    // Las butacas especiales, de código a tipo, sobre una distribución ya validada. Los códigos llegan
    // como los tipeó el encargado (" a1"): se llevan a la forma de Asiento.codigoDe, y uno vacío no
    // nombra ninguna butaca. Una butaca en dos listas no tiene un tipo que elegir: antes ganaba la última
    // en silencio. Y una que no cae en la distribución es un código mal tipeado: ignorarla dejaba la
    // sala sin la butaca que pidió el encargado, sin avisarle.
    public static Map<String, TipoAsiento> especiales(List<Integer> butacasPorFila,
                                                      Map<TipoAsiento, List<String>> especiales) {
        Map<String, TipoAsiento> porCodigo = new LinkedHashMap<>();
        especiales.forEach((tipo, codigos) -> {
            if (codigos != null) {
                codigos.stream().map(Asiento::normalizarCodigo).filter(codigo -> !codigo.isEmpty())
                        .forEach(codigo -> marcar(porCodigo, codigo, tipo));
            }
        });
        Set<String> existentes = codigos(butacasPorFila);
        porCodigo.keySet().stream().filter(codigo -> !existentes.contains(codigo)).findFirst()
                .ifPresent(codigo -> {
                    throw new DatoInvalido(Asiento.inexistente(codigo));
                });
        return porCodigo;
    }

    private static void marcar(Map<String, TipoAsiento> porCodigo, String codigo, TipoAsiento tipo) {
        TipoAsiento anterior = porCodigo.put(codigo, tipo);
        if (anterior != null && anterior != tipo) {
            throw new DatoInvalido("La butaca " + codigo
                    + " está en más de una lista de especiales: dejala en una sola");
        }
    }

    private static Set<String> codigos(List<Integer> butacasPorFila) {
        Set<String> codigos = new HashSet<>();
        for (int fila = 1; fila <= butacasPorFila.size(); fila++) {
            for (int numero = 1; numero <= butacasPorFila.get(fila - 1); numero++) {
                codigos.add(Asiento.codigoDe(fila, numero));
            }
        }
        return codigos;
    }
}
