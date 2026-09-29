package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;
import static ar.uade.cine.swing.comun.Formato.escapar;

/** El buzón de lo que trajo el importador: hasta que se confirma, no se programa ni lo ve el cliente. */
public final class PantallaPendientes extends Pantalla {

    private final ApiCartelera apiCartelera;
    private final Navegacion navegacion;
    private final JPanel tarjetas = new JPanel(new GridLayout(0, 2, 12, 12));

    public PantallaPendientes(ApiCartelera apiCartelera, Navegacion navegacion) {
        super("Por revisar", "Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no "
                + "se pueden programar ni las ve el cliente. Lo que descartes queda descartado: el importador no lo "
                + "vuelve a proponer.");
        this.apiCartelera = apiCartelera;
        this.navegacion = navegacion;

        JPanel arriba = new AlAnchoDelVisor(new BorderLayout(), false);
        arriba.add(tarjetas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(arriba);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        recargar();
    }

    private void recargar() {
        cargar(apiCartelera::obtenerPeliculasPendientes, this::pintar);
    }

    private void pintar(List<Pelicula> pendientes) {
        tarjetas.removeAll();
        if (pendientes.isEmpty()) {
            JPanel vacio = new JPanel(new FlujoConSalto());
            vacio.add(Componentes.nota("No hay nada esperando. Cuando el importador traiga títulos nuevos van a "
                    + "aparecer acá."));
            JButton importar = new JButton("Traer cartelera ahora");
            importar.addActionListener(e -> navegacion.ir(Destino.IMPORTADOR));
            vacio.add(importar);
            tarjetas.add(vacio);
        }
        pendientes.forEach(p -> tarjetas.add(tarjeta(p)));
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private JPanel tarjeta(Pelicula pelicula) {
        // Sin ancho fijo: corta línea al de la tarjeta, que es la mitad de lo que haya.
        JLabel datos = Componentes.texto("<b style='font-size:13pt'>"
                + escapar(pelicula.titulo()) + "</b><br>"
                + (pelicula.anio() > 0 ? pelicula.anio() : "—") + " · " + duracion(pelicula.duracionMinutos())
                + " · " + etiqueta(pelicula.clasificacion()) + "<br>"
                + escapar(vacioSi(pelicula.director(), "Sin director")) + "<br>"
                + pelicula.generos().stream().map(g -> etiqueta(g)).collect(Collectors.joining(", "))
                + "<p style='margin-top:6px'>" + escapar(vacioSi(pelicula.sinopsis(), "Sin sinopsis.")) + "</p>");
        JButton confirmar = new JButton("Confirmar");
        JButton descartar = new JButton("Descartar");
        JPanel botones = new JPanel(new GridLayout(1, 2, 6, 0));
        botones.add(confirmar);
        botones.add(descartar);
        // Deshabilitados mientras viaja: dos clics seguidos mandarían confirmar y descartar la misma película.
        confirmar.addActionListener(e -> decidir(confirmar, descartar,
                () -> apiCartelera.confirmarPelicula(pelicula.id()),
                pelicula.titulo() + " confirmada: ya se puede programar"));
        descartar.addActionListener(e -> {
            // No tiene vuelta atrás: el importador no la vuelve a proponer.
            if (!confirmar("¿Descartar " + pelicula.titulo() + "? El importador no la vuelve a proponer.",
                    "Sí, descartar")) return;
            decidir(confirmar, descartar, () -> apiCartelera.descartarPelicula(pelicula.id()),
                    pelicula.titulo() + " descartada");
        });

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
            Mensajes.error(this, error);
        });
    }

    private static String vacioSi(String valor, String reemplazo) {
        return valor == null || valor.isBlank() ? reemplazo : valor;
    }
}
