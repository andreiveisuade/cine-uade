package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ApiImagenes;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

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

    // ImageIO no lee WEBP: una imagen subida en ese formato queda sin vista.
    static BufferedImage leer(byte[] datos) {
        try {
            return ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException e) {
            return null;
        }
    }
}
