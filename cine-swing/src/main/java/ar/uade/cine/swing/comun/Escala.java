package ar.uade.cine.swing.comun;

// Los espacios, los tamaños de letra y el redondeo del panel; como Colores, pero de medidas.
// Sin números sueltos en las pantallas: todas respiran igual y un ajuste se hace en un solo lugar.
public final class Escala {

    // Los espacios, base 4: del más pegado al margen de la pantalla.
    public static final int PEGADO = 4;
    public static final int JUNTO = 8;
    public static final int CERCA = 12;
    public static final int APARTE = 16;
    public static final int BLOQUE = 24;
    public static final int MARGEN = 32;

    // La letra: cifra de caja, título de pantalla, de tarjeta, cuerpo (la del tema), nota y rótulo.
    public static final float LETRA_CIFRA = 28f;
    public static final float LETRA_TITULO = 22f;
    public static final float LETRA_TARJETA = 16f;
    public static final float LETRA_NOTA = 12f;
    public static final float LETRA_ROTULO = 11f;

    /** El redondeo de tarjetas y recuadros. */
    public static final int ARCO = 12;

    private Escala() {
    }
}
