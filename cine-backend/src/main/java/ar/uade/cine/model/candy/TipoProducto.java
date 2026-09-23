package ar.uade.cine.model.candy;

public enum TipoProducto {
    POCHOCLOS,
    BEBIDA,
    GOLOSINA,
    COMBO;

    // El combo no se da de alta con un tipo: se arma declarando qué trae.
    public boolean esCombo() {
        return this == COMBO;
    }
}
