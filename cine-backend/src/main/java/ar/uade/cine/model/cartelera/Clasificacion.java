package ar.uade.cine.model.cartelera;

import lombok.Getter;

@Getter
public enum Clasificacion {

    ATP(0),
    MAS_13(13),
    MAS_16(16),
    MAS_18(18);

    private final int edadMinima;

    Clasificacion(int edadMinima) {
        this.edadMinima = edadMinima;
    }

    public String getEtiqueta() {
        return edadMinima == 0 ? "ATP" : "+" + edadMinima;
    }

    @Override
    public String toString() {
        return getEtiqueta();
    }
}
