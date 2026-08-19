package ar.uade.cine.model.cartelera;

/**
 * Clasificación por edad. Guarda la edad mínima para que la validación al vender —cuando
 * se implemente— no tenga que traducir la etiqueta a un número con un switch.
 */
public enum Clasificacion {

    ATP(0),
    MAS_13(13),
    MAS_16(16),
    MAS_18(18);

    private final int edadMinima;

    Clasificacion(int edadMinima) {
        this.edadMinima = edadMinima;
    }

    public int getEdadMinima() {
        return edadMinima;
    }

    /** Cómo se muestra en la cartelera: "ATP", "+13", "+16", "+18". */
    public String getEtiqueta() {
        return edadMinima == 0 ? "ATP" : "+" + edadMinima;
    }

    /** Para mostrarla. Lo que se persiste es name(), que no cambia. */
    @Override
    public String toString() {
        return getEtiqueta();
    }
}
