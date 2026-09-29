package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.informes.TicketCandy;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

// El ticket de la última venta de mostrador, como lo escribe TicketCandy; antes de cobrar, de dónde sale.
final class TicketVenta extends JPanel {

    private final JLabel titulo = new JLabel(" ");
    // Cuarenta columnas: lo justo para el renglón del ticket, que mide TicketCandy.
    private final JTextArea ticket = Componentes.areaDeLectura(18, 40);

    TicketVenta() {
        super(new BorderLayout(0, 6));
        ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ticket.setText("El total lo calcula el backend con los precios de la carta: acá no se tipea.\n"
                + "Al cobrar aparece el ticket.");
        add(titulo, BorderLayout.NORTH);
        add(new JScrollPane(ticket), BorderLayout.CENTER);
    }

    void mostrar(CompraCandy compra) {
        titulo.setText("Venta #" + compra.id());
        ticket.setText(TicketCandy.escribir(compra));
    }
}
