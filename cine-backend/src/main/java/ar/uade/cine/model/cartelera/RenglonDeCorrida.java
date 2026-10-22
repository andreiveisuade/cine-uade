package ar.uade.cine.model.cartelera;

import java.util.ArrayList;
import java.util.List;

// Una película de una corrida del importador y qué le pasó; dueño del renglón de texto que la guarda.
// Escribe y lee el mismo formato: así nadie fuera de acá parsea el detalle.
public record RenglonDeCorrida(Resultado resultado, Integer peliculaId, String titulo, String motivo) {

    public enum Resultado { NUEVA, YA_ESTABA, RECHAZADA }

    private static final String NUEVA = "+ [";
    private static final String YA_ESTABA = "= ";
    private static final String RECHAZADA = "✗ ";
    // Tabulación y no «: »: un título como «Sinsajo: Parte 1» lo partiría mal.
    private static final char ANTES_DEL_MOTIVO = '\t';

    public static RenglonDeCorrida nueva(int peliculaId, String titulo) {
        return new RenglonDeCorrida(Resultado.NUEVA, peliculaId, titulo, null);
    }

    public static RenglonDeCorrida yaEstaba(String titulo) {
        return new RenglonDeCorrida(Resultado.YA_ESTABA, null, titulo, null);
    }

    public static RenglonDeCorrida rechazada(String titulo, String motivo) {
        return new RenglonDeCorrida(Resultado.RECHAZADA, null, titulo, motivo);
    }

    public String texto() {
        return switch (resultado) {
            case NUEVA -> NUEVA + peliculaId + "] " + titulo;
            case YA_ESTABA -> YA_ESTABA + titulo;
            case RECHAZADA -> RECHAZADA + titulo + ANTES_DEL_MOTIVO + motivo;
        };
    }

    // Saltea lo que no es un renglón: el detalle de una corrida fallida es solo su motivo.
    public static List<RenglonDeCorrida> leer(String detalle) {
        List<RenglonDeCorrida> renglones = new ArrayList<>();
        if (detalle == null) return renglones;
        for (String linea : detalle.split("\n")) {
            RenglonDeCorrida renglon = leerUno(linea);
            if (renglon != null) renglones.add(renglon);
        }
        return renglones;
    }

    private static RenglonDeCorrida leerUno(String linea) {
        if (linea.startsWith(NUEVA)) {
            int cierre = linea.indexOf("] ");
            if (cierre < 0) return null;
            try {
                return nueva(Integer.parseInt(linea.substring(NUEVA.length(), cierre)), linea.substring(cierre + 2));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (linea.startsWith(YA_ESTABA)) return yaEstaba(linea.substring(YA_ESTABA.length()));
        if (linea.startsWith(RECHAZADA)) {
            String resto = linea.substring(RECHAZADA.length());
            int corte = resto.indexOf(ANTES_DEL_MOTIVO);
            // Las corridas viejas separaban con «: » y no se puede saber dónde termina el título.
            return corte < 0 ? rechazada(resto, null)
                    : rechazada(resto.substring(0, corte), resto.substring(corte + 1));
        }
        return null;
    }
}
