package ar.uade.cine.model.candy;

// Rubro de un producto de la carta; Experto en si es combo, que solo nace declarando qué trae.
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
