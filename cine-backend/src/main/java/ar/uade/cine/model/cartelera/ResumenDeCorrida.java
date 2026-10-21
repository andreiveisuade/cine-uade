package ar.uade.cine.model.cartelera;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Lo que dejó una corrida del importador: cuántas entraron, se saltearon o fallaron, y el renglón de cada una.
// Introduce Parameter Object: los tres contadores y el detalle viajaban sueltos y juntos hasta
// Importacion.terminar, que tenía cinco parámetros (Long Parameter List, Data Clump).
// Record @Embeddable como FormatoDeFuncion: sus columnas se llaman como los componentes, las mismas de antes.
@Embeddable
public record ResumenDeCorrida(int nuevas, int salteadas, int fallidas,
                               @Column(columnDefinition = "TEXT") String detalle) {

    // Una corrida que falló antes de mirar candidatas no contó nada: solo queda el motivo.
    public static ResumenDeCorrida deFalla(String motivo) {
        return new ResumenDeCorrida(0, 0, 0, motivo);
    }
}
