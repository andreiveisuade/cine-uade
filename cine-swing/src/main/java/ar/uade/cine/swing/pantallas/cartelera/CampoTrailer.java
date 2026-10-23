package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.comun.Lecturas;
import ar.uade.cine.swing.comun.Mensajes;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

// El trailer de la película con su botón Ver: lo trae el importador y el campo queda para corregirlo.
final class CampoTrailer extends JPanel {

    private final JTextField trailer = new JTextField();

    CampoTrailer() {
        super(new BorderLayout(4, 0));
        JButton ver = new JButton("Ver");
        ver.addActionListener(e -> ver());
        add(trailer, BorderLayout.CENTER);
        add(ver, BorderLayout.EAST);
    }

    String texto() {
        return trailer.getText().trim();
    }

    void poner(String texto) {
        trailer.setText(texto);
    }

    // Abre lo escrito, aunque no esté guardado.
    private void ver() {
        String direccion = Lecturas.direccionDelTrailer(trailer.getText());
        if (direccion == null) {
            Mensajes.error(this, "Falta el trailer: pegá el link de YouTube");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(direccion));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException e) {
            Mensajes.error(this, "No se pudo abrir el navegador: copiá el link y abrilo a mano");
        }
    }
}
