package ar.uade.cine.swing.comun;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.function.Supplier;

// Los colores de estado y de tipo de butaca, cada uno con su par claro y oscuro; el resto sale del tema.
// El tema son los .properties de resources/ar/uade/cine/swing/tema: los neutros y el ámbar de la web.
// Cada color se resuelve al pintar, no al crearse: cambiar el tema no obliga a rearmar (y recargar) la pantalla.
public final class Colores {

    private static final String TEMA = "ar.uade.cine.swing.tema";

    private Colores() {
    }

    /** Pone el tema y redibuja lo abierto: los colores de acá se leen al pintar, así que no hay que recrear nada. */
    public static void usarTema(boolean oscuro) {
        FlatLaf.registerCustomDefaultsSource(TEMA);
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
    }

    public static boolean oscuro() {
        return FlatLaf.isLafDark();
    }

    private static Color par(int claro, int oscuro) {
        return new Color(oscuro() ? oscuro : claro);
    }

    public static Color error() {
        return vivo(() -> par(0xC92A2A, 0xFF6B6B));
    }

    public static Color exito() {
        return vivo(() -> par(0x2B8A3E, 0x69DB7C));
    }

    public static Color aviso() {
        return vivo(() -> par(0xE67700, 0xFFD43B));
    }

    public static Color texto() {
        return vivo(() -> UIManager.getColor("Label.foreground"));
    }

    public static Color secundario() {
        return vivo(() -> {
            Color color = UIManager.getColor("Label.disabledForeground");
            return color != null ? color : Color.GRAY;
        });
    }

    public static Color borde() {
        return vivo(() -> {
            Color color = UIManager.getColor("Component.borderColor");
            return color != null ? color : Color.GRAY;
        });
    }

    public static Color fondo() {
        return vivo(() -> UIManager.getColor("Panel.background"));
    }

    /** El menú de la izquierda: un paso más hondo que la pantalla, como el costado de la web. */
    public static Color lateral() {
        return vivo(() -> {
            Color color = UIManager.getColor("Cine.lateral.background");
            return color != null ? color : fondo();
        });
    }

    public static Color seleccion() {
        return vivo(() -> UIManager.getColor("Table.selectionBackground"));
    }

    // Tipos de butaca: tintes suaves en claro, tonos apagados en oscuro, para que el número siga legible.
    public static Color butaca(String tipo) {
        return vivo(() -> {
            return switch (tipo == null ? "" : tipo) {
                case "VIP" -> par(0xFFF3BF, 0x5C4B12);
                case "PAREJA" -> par(0xFFDEEB, 0x5C2440);
                case "ACCESIBLE" -> par(0xD0EBFF, 0x1C3F5E);
                default -> UIManager.getColor("Button.background");
            };
        });
    }

    public static Color butacaFueraDeServicio() {
        return vivo(() -> par(0xE9ECEF, 0x262626));
    }

    // Semitransparente: el rayado de la limpieza en la agenda deja ver la grilla de horas de abajo.
    public static Color limpieza() {
        return vivo(() -> translucido(par(0x64748B, 0x94A3B8), 70));
    }

    public static Color pantallaDeSala() {
        return vivo(() -> par(0x343A40, 0xADB5BD));
    }

    public static Color textoPantallaDeSala() {
        return vivo(() -> par(0xFFFFFF, 0x1E1E1E));
    }

    // Un color por película: pastel en claro, mezclado con el fondo en oscuro.
    private static final int[][] PELICULAS = {
            {0xC3FAE8, 0x0CA678}, {0xD0EBFF, 0x1C7ED6}, {0xFFF3BF, 0xF59F00}, {0xFFDEEB, 0xD6336C},
            {0xE5DBFF, 0x7048E8}, {0xC5F6FA, 0x1098AD}, {0xFFE8CC, 0xF76707}, {0xDBE4FF, 0x4263EB}};

    public static Color fondoPelicula(int id) {
        return vivo(() -> {
            int[] par = PELICULAS[Math.floorMod(id, PELICULAS.length)];
            return oscuro() ? mezclar(new Color(par[1]), fondo(), 0.35) : new Color(par[0]);
        });
    }

    public static Color bordePelicula(int id) {
        return vivo(() -> new Color(PELICULAS[Math.floorMod(id, PELICULAS.length)][1]));
    }

    public static Color textoPelicula() {
        return vivo(() -> oscuro() ? texto() : new Color(0x212529));
    }

    private static Color mezclar(Color color, Color base, double proporcion) {
        return new Color(
                (int) Math.round(color.getRed() * proporcion + base.getRed() * (1 - proporcion)),
                (int) Math.round(color.getGreen() * proporcion + base.getGreen() * (1 - proporcion)),
                (int) Math.round(color.getBlue() * proporcion + base.getBlue() * (1 - proporcion)));
    }

    /** El mismo color dejando ver lo de abajo: {@code alfa} va de 0 (invisible) a 255 (opaco). */
    public static Color translucido(Color color, int alfa) {
        return vivo(() -> new Color(color.getRed(), color.getGreen(), color.getBlue(), alfa));
    }

    private static Color vivo(Supplier<Color> color) {
        return new ColorDelTema(color);
    }

    // Java2D pinta con getRGB(), y getRed()/getAlpha() salen de ahí: alcanza con pedirle el color al tema cada vez.
    private static final class ColorDelTema extends Color {
        private final transient Supplier<Color> color;

        ColorDelTema(Supplier<Color> color) {
            super(0);
            this.color = color;
        }

        @Override
        public int getRGB() {
            Color ahora = color.get();
            return ahora != null ? ahora.getRGB() : Color.GRAY.getRGB();
        }
    }

    /** Para meter un color del tema en el HTML de un JLabel. */
    public static String hex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }
}
