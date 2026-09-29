package ar.uade.cine.swing.comun;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

// Guarda en la PC del encargado lo que se entrega al INCAA: el backend da los números, el archivo sale de acá.
public final class Archivos {

    private Archivos() {
    }

    /**
     * Pregunta dónde, con {@code sugerido} como nombre, y escribe en UTF-8. Vacío si el encargado canceló o si no se
     * pudo escribir: eso último ya se avisó con un diálogo.
     */
    public static Optional<Path> guardar(Component origen, String sugerido, String contenido, String que) {
        JFileChooser elegir = new JFileChooser();
        elegir.setSelectedFile(new File(sugerido));
        if (elegir.showSaveDialog(origen) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        Path destino = elegir.getSelectedFile().toPath();
        try {
            Files.writeString(destino, contenido, StandardCharsets.UTF_8);
            return Optional.of(destino);
        } catch (IOException e) {
            Mensajes.error(origen, "No se pudo guardar " + que + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
