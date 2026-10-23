package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.dto.cartelera.Importacion;
import ar.uade.cine.swing.api.dto.cartelera.PeliculaDeCorrida;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

// El detalle de una corrida del importador: qué pasó con cada película y el atajo a las pendientes.
final class DetalleCorrida extends JPanel {

    private static final int POR_PAGINA = 20;

    private final JLabel titulo = Componentes.subtitulo(" ");
    private final JLabel resumen = Componentes.texto(" ");
    private final JButton revisar = new JButton();
    private final Tabla<PeliculaDeCorrida> peliculas = new Tabla<>(
            Columna.<PeliculaDeCorrida>de("Resultado", p -> etiqueta(p.resultado())).ancho(90),
            Columna.<PeliculaDeCorrida>de("Película", PeliculaDeCorrida::titulo).ancho(280),
            Columna.<PeliculaDeCorrida>de("Qué pasó", DetalleCorrida::quePaso).ancho(300));

    DetalleCorrida(Runnable alRevisar) {
        super(new BorderLayout(0, 6));
        revisar.setVisible(false);
        revisar.addActionListener(e -> alRevisar.run());
        JPanel encabezado = new JPanel(new BorderLayout(12, 0));
        encabezado.add(new Pila().agregar(titulo, resumen), BorderLayout.CENTER);
        encabezado.add(revisar, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);
        add(peliculas.conScroll());
        mostrar(null);
    }

    void mostrar(Importacion corrida) {
        if (corrida == null) {
            titulo.setText("Todavía no se trajo nada");
            resumen.setText("Elegí cuántas películas y apretá Traer cartelera.");
            resumen.setForeground(Colores.secundario());
            revisar.setVisible(false);
            peliculas.mostrar(List.of());
            return;
        }
        titulo.setText("Corrida del " + fechaHora(corrida.pedidaEn()));
        boolean fallo = "FALLIDA".equals(corrida.estado());
        resumen.setText(fallo ? "Falló: " + (corrida.detalle() == null ? "sin motivo" : corrida.detalle())
                : "Pediste hasta " + corrida.paginas() * POR_PAGINA + ". " + resumen(corrida) + ".");
        resumen.setForeground(fallo ? Colores.error() : Colores.texto());
        long pendientes = corrida.peliculas().stream().filter(p -> "PENDIENTE".equals(p.estadoRevision())).count();
        revisar.setText("Revisar " + (pendientes == 1 ? "la pendiente" : "las " + pendientes + " pendientes"));
        revisar.setVisible(pendientes > 0);
        peliculas.mostrar(corrida.peliculas());
    }

    static String resumen(Importacion corrida) {
        return cantidad(corrida.nuevas(), "nueva", "nuevas") + ", "
                + cantidad(corrida.salteadas(), "ya estaba", "ya estaban") + ", "
                + cantidad(corrida.fallidas(), "rechazada", "rechazadas");
    }

    private static String quePaso(PeliculaDeCorrida p) {
        return switch (p.resultado()) {
            case "NUEVA" -> p.estadoRevision() == null ? "Entró, después se borró"
                    : "Entró · hoy: " + etiqueta(p.estadoRevision());
            case "YA_ESTABA" -> "Ya estaba cargada: no se tocó";
            default -> p.motivo() == null ? "No pasó las reglas del cine" : p.motivo();
        };
    }
}
