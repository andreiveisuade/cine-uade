package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Pelicula;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;

/** El buzón de lo que trajo el importador: hasta que se confirma, no se programa ni lo ve el cliente. */
final class PantallaPendientes extends Pantalla {

    private final Navegacion navegacion;
    private final JPanel tarjetas = new JPanel(new GridLayout(0, 2, 12, 12));

    PantallaPendientes(ApiHttp api, Navegacion navegacion) {
        super(api, "Por revisar", "Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no "
                + "se pueden programar ni las ve el cliente. Lo que descartes queda descartado: el importador no lo "
                + "vuelve a proponer.");
        this.navegacion = navegacion;
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(tarjetas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(arriba);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        recargar();
    }

    private void recargar() {
        cargar(api::obtenerPeliculasPendientes, this::pintar);
    }

    private void pintar(List<Pelicula> pendientes) {
        tarjetas.removeAll();
        if (pendientes.isEmpty()) {
            JPanel vacio = new JPanel(new FlowLayout(FlowLayout.LEFT));
            vacio.add(Componentes.nota("No hay nada esperando. Cuando el importador traiga títulos nuevos van a "
                    + "aparecer acá."));
            JButton importar = new JButton("Traer cartelera ahora");
            importar.addActionListener(e -> navegacion.ir("Importador"));
            vacio.add(importar);
            tarjetas.add(vacio);
        }
        pendientes.forEach(p -> tarjetas.add(tarjeta(p)));
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private JPanel tarjeta(Pelicula pelicula) {
        JLabel datos = new JLabel("<html><div style='width:300px'><b style='font-size:13pt'>"
                + escapar(pelicula.titulo()) + "</b><br>"
                + (pelicula.anio() > 0 ? pelicula.anio() : "—") + " · " + duracion(pelicula.duracionMinutos())
                + " · " + etiqueta(pelicula.clasificacion()) + "<br>"
                + escapar(vacioSi(pelicula.director(), "Sin director")) + "<br>"
                + pelicula.generos().stream().map(g -> etiqueta(g)).collect(Collectors.joining(", "))
                + "<p style='margin-top:6px'>" + escapar(vacioSi(pelicula.sinopsis(), "Sin sinopsis.")) + "</p>"
                + "</div></html>");
        JButton confirmar = new JButton("Confirmar");
        JButton descartar = new JButton("Descartar");
        JPanel botones = new JPanel(new GridLayout(1, 2, 6, 0));
        botones.add(confirmar);
        botones.add(descartar);
        // Deshabilitados mientras viaja: dos clics seguidos mandarían confirmar y descartar la misma película.
        confirmar.addActionListener(e -> decidir(confirmar, descartar, () -> api.confirmarPelicula(pelicula.id()),
                pelicula.titulo() + " confirmada: ya se puede programar"));
        descartar.addActionListener(e -> decidir(confirmar, descartar, () -> api.descartarPelicula(pelicula.id()),
                pelicula.titulo() + " descartada"));

        JPanel tarjeta = new JPanel(new BorderLayout(0, 8));
        tarjeta.add(datos, BorderLayout.CENTER);
        tarjeta.add(botones, BorderLayout.SOUTH);
        return Componentes.conBorde(tarjeta);
    }

    private void decidir(JButton confirmar, JButton descartar, Callable<Pelicula> pedido,
                         String mensaje) {
        confirmar.setEnabled(false);
        descartar.setEnabled(false);
        Tarea.ejecutar(this, pedido, hecha -> {
            avisar(mensaje);
            recargar();
        }, error -> {
            confirmar.setEnabled(true);
            descartar.setEnabled(true);
            if (!error.esSesionVencida()) Tarea.mostrarError(this, error);
        });
    }

    private static String vacioSi(String valor, String reemplazo) {
        return valor == null || valor.isBlank() ? reemplazo : valor;
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
