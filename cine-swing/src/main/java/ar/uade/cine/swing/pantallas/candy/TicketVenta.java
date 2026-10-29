package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.informes.TicketCandy;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

// El ticket de la última venta de mostrador, como lo escribe TicketCandy; antes de cobrar, de dónde sale.
final class TicketVenta extends JPanel {

    private final JLabel titulo = Componentes.subtitulo("Ticket");
    // El renglón del ticket mide 40 (TicketCandy); dos más por el margen del área, así no aparece el scroll.
    private final JTextArea ticket = Componentes.areaDeLectura(16, 42);

    TicketVenta() {
        super(new BorderLayout(0, Escala.JUNTO));
        ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        // Es texto con columnas fijas: cortar la línea partía el renglón de "=" en dos. Si no entra, scroll.
        ticket.setLineWrap(false);
        ticket.setText("El total sale de los precios de la carta.\nAl cobrar aparece acá el ticket.");
        add(titulo, BorderLayout.NORTH);
        add(new JScrollPane(ticket), BorderLayout.CENTER);
    }

    void mostrar(CompraCandy compra) {
        titulo.setText("Venta #" + compra.id());
        ticket.setText(TicketCandy.escribir(compra));
    }
}
