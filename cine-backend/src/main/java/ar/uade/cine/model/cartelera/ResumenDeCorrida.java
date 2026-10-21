package ar.uade.cine.model.cartelera;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Lo que dejó una corrida del importador: cuántas entraron, se saltearon o fallaron, y el renglón de cada una.
// Parameter Object: contadores y detalle viajaban sueltos hasta Importacion.terminar (cinco parámetros).
// Record @Embeddable como FormatoDeFuncion.
@Embeddable
public record ResumenDeCorrida(int nuevas, int salteadas, int fallidas,
                               @Column(columnDefinition = "TEXT") String detalle) {

    // Una corrida que falló antes de mirar candidatas no contó nada: solo queda el motivo.
    public static ResumenDeCorrida deFalla(String motivo) {
        return new ResumenDeCorrida(0, 0, 0, motivo);
    }
}
