package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ApiImagenes;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// La imagen de una promo o un producto: se sube apenas se elige y el formulario guarda solo la URL que devolvió.
public final class CampoImagen extends JPanel {

    private static final int ANCHO = 160;
    private static final int ALTO = 120;

    private final ApiImagenes api;
    private final JLabel vista = new JLabel("Sin imagen", SwingConstants.CENTER);
    private final JButton quitar = new JButton("Quitar");
    private final JLabel error = Componentes.texto(" ");
    private String url;

    public CampoImagen(ApiImagenes api) {
        super(new BorderLayout(0, 4));
        this.api = api;
        vista.setPreferredSize(new Dimension(ANCHO, ALTO));
        JButton elegir = new JButton("Elegir imagen…");
        elegir.addActionListener(e -> elegir());
        quitar.addActionListener(e -> poner(null));
        add(Componentes.izquierda(vista), BorderLayout.NORTH);
        add(Componentes.botones(elegir, quitar), BorderLayout.CENTER);
        add(error, BorderLayout.SOUTH);
        poner(null);
    }

    /** Null si no hay imagen: así viaja en el pedido. */
    public String url() {
        return url;
    }

    /** La de una promo o producto que se edita; la vista previa se baja del backend. */
    public void poner(String nueva) {
        url = nueva == null || nueva.isBlank() ? null : nueva;
        error.setText(" ");
        quitar.setEnabled(url != null);
        mostrar(null);
        if (url == null) return;
        String pedida = url;
        Tarea.ejecutar(this, () -> api.descargar(pedida), datos -> {
            // Si mientras bajaba se eligió otra, esta ya no corresponde.
            if (pedida.equals(url)) mostrar(leer(datos));
        }, e -> vista.setText("No se pudo cargar"));
    }

    private void elegir() {
        JFileChooser elegir = new JFileChooser();
        elegir.setFileFilter(new FileNameExtensionFilter("Imágenes (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
        if (elegir.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path archivo = elegir.getSelectedFile().toPath();
        Tarea.ejecutar(this, () -> api.subir(archivo), subida -> {
            url = subida;
            error.setText(" ");
            quitar.setEnabled(true);
            mostrar(leer(archivo));
        }, e -> new Validacion(error).mostrarError(e));
    }

    private void mostrar(BufferedImage imagen) {
        vista.setText(imagen == null ? (url == null ? "Sin imagen" : "Sin vista previa") : null);
        vista.setIcon(imagen == null ? null : new ImageIcon(escalada(imagen)));
    }

    private static Image escalada(BufferedImage imagen) {
        double escala = Math.min((double) ANCHO / imagen.getWidth(), (double) ALTO / imagen.getHeight());
        return imagen.getScaledInstance((int) Math.max(1, imagen.getWidth() * escala),
                (int) Math.max(1, imagen.getHeight() * escala), Image.SCALE_SMOOTH);
    }

    // ImageIO no lee WEBP: esa queda subida pero sin vista previa.
    private static BufferedImage leer(byte[] datos) {
        try {
            return ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage leer(Path archivo) {
        try {
            return leer(Files.readAllBytes(archivo));
        } catch (IOException e) {
            return null;
        }
    }
}
