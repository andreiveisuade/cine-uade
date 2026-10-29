package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ApiImagenes;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// El póster de una película a tamaño fijo; se baja aparte, así la pantalla no espera a que lleguen todos.
public final class Poster extends JLabel {

    public static final int ANCHO = 120;
    public static final int ALTO = 180;

    public Poster(ApiImagenes api, String url) {
        super("Sin póster", SwingConstants.CENTER);
        setForeground(Colores.secundario());
        setBorder(BorderFactory.createLineBorder(Colores.borde()));
        Dimension tamano = new Dimension(ANCHO, ALTO);
        setPreferredSize(tamano);
        setMinimumSize(tamano);
        setMaximumSize(tamano);
        if (url == null || url.isBlank()) return;
        setText("Cargando…");
        Tarea.ejecutar(this, () -> leer(api.descargar(url)), imagen -> {
            setText(imagen == null ? "Sin póster" : null);
            if (imagen != null) setIcon(new ImageIcon(imagen.getScaledInstance(ANCHO, ALTO, Image.SCALE_SMOOTH)));
        }, e -> setText("Sin póster"));
    }

    // Una por URL en toda la sesión: volver a Películas no las baja de nuevo.
    private static final Map<String, ImageIcon> MINIATURAS = new ConcurrentHashMap<>();

    /** La miniatura de una fila de tabla, de {@code alto} px; llega sola, cuando se bajó. */
    public static void miniatura(Component origen, ApiImagenes api, String url, int alto, Consumer<Icon> alLlegar) {
        if (url == null || url.isBlank()) return;
        ImageIcon guardada = MINIATURAS.get(url);
        if (guardada != null) {
            alLlegar.accept(guardada);
            return;
        }
        Tarea.ejecutar(origen, () -> leer(api.descargar(url)), imagen -> {
            if (imagen == null) return;
            ImageIcon icono = new ImageIcon(imagen.getScaledInstance(alto * ANCHO / ALTO, alto, Image.SCALE_SMOOTH));
            MINIATURAS.put(url, icono);
            alLlegar.accept(icono);
        }, e -> { });
    }

    // ImageIO no lee WEBP: una imagen subida en ese formato queda sin vista.
    static BufferedImage leer(byte[] datos) {
        try {
            return ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException e) {
            return null;
        }
    }
}
