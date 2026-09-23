package ar.uade.cine.swing.comun;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Los pocos colores que no salen del look and feel: estados (error, éxito, aviso) y los tipos de butaca. Cada uno
 * tiene su par claro y oscuro, porque un rojo que se lee sobre blanco se pierde sobre gris oscuro. El resto de la
 * pantalla usa los colores del tema vía UIManager.
 */
public final class Colores {

    private Colores() {
    }

    private static boolean oscuro() {
        return FlatLaf.isLafDark();
    }

    private static Color par(int claro, int oscuro) {
        return new Color(oscuro() ? oscuro : claro);
    }

    public static Color error() {
        return par(0xC92A2A, 0xFF6B6B);
    }

    public static Color exito() {
        return par(0x2B8A3E, 0x69DB7C);
    }

    public static Color aviso() {
        return par(0xE67700, 0xFFD43B);
    }

    public static Color texto() {
        return UIManager.getColor("Label.foreground");
    }

    public static Color secundario() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? color : Color.GRAY;
    }

    public static Color borde() {
        Color color = UIManager.getColor("Component.borderColor");
        return color != null ? color : Color.GRAY;
    }

    public static Color fondo() {
        return UIManager.getColor("Panel.background");
    }

    public static Color seleccion() {
        return UIManager.getColor("Table.selectionBackground");
    }

    // Tipos de butaca: tintes suaves en claro, tonos apagados en oscuro, para que el número siga legible.
    public static Color butaca(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "VIP" -> par(0xFFF3BF, 0x5C4B12);
            case "PAREJA" -> par(0xFFDEEB, 0x5C2440);
            case "ACCESIBLE" -> par(0xD0EBFF, 0x1C3F5E);
            default -> UIManager.getColor("Button.background");
        };
    }

    public static Color butacaFueraDeServicio() {
        return par(0xE9ECEF, 0x262626);
    }

    // Semitransparente: el rayado de la limpieza en la agenda deja ver la grilla de horas de abajo.
    public static Color limpieza() {
        Color base = par(0x64748B, 0x94A3B8);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), 70);
    }

    public static Color pantallaDeSala() {
        return par(0x343A40, 0xADB5BD);
    }

    public static Color textoPantallaDeSala() {
        return par(0xFFFFFF, 0x1E1E1E);
    }

    // Un color por película en la agenda: pastel con texto oscuro en claro; en oscuro, el mismo tono mezclado con el
    // fondo, para que los bloques no encandilen y el texto del tema se siga leyendo.
    private static final int[][] PELICULAS = {
            {0xC3FAE8, 0x0CA678}, {0xD0EBFF, 0x1C7ED6}, {0xFFF3BF, 0xF59F00}, {0xFFDEEB, 0xD6336C},
            {0xE5DBFF, 0x7048E8}, {0xC5F6FA, 0x1098AD}, {0xFFE8CC, 0xF76707}, {0xDBE4FF, 0x4263EB}};

    public static Color fondoPelicula(int id) {
        int[] par = PELICULAS[Math.floorMod(id, PELICULAS.length)];
        return oscuro() ? mezclar(new Color(par[1]), fondo(), 0.35) : new Color(par[0]);
    }

    public static Color bordePelicula(int id) {
        return new Color(PELICULAS[Math.floorMod(id, PELICULAS.length)][1]);
    }

    public static Color textoPelicula() {
        return oscuro() ? texto() : new Color(0x212529);
    }

    private static Color mezclar(Color color, Color base, double proporcion) {
        return new Color(
                (int) Math.round(color.getRed() * proporcion + base.getRed() * (1 - proporcion)),
                (int) Math.round(color.getGreen() * proporcion + base.getGreen() * (1 - proporcion)),
                (int) Math.round(color.getBlue() * proporcion + base.getBlue() * (1 - proporcion)));
    }

    /** Para meter un color del tema en el HTML de un JLabel. */
    public static String hex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }
}
