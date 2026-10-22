package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Poster;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

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
import static ar.uade.cine.swing.comun.Formato.escapar;

// El buzón de lo que trajo el importador: hasta que se confirma, no se programa ni lo ve el cliente.
public final class PantallaPendientes extends Pantalla {

    private final ApiCartelera apiCartelera;
    private final ApiImagenes apiImagenes;
    private final Navegacion navegacion;
    private final JPanel tarjetas = new JPanel(new GridLayout(0, 1, 0, 12));

    public PantallaPendientes(ApiCartelera apiCartelera, ApiImagenes apiImagenes, Navegacion navegacion) {
        super("Por revisar", "Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no "
                + "se pueden programar ni las ve el cliente. Lo que descartes queda descartado: el importador no lo "
                + "vuelve a proponer.");
        this.apiCartelera = apiCartelera;
        this.apiImagenes = apiImagenes;
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
        pendientes.forEach(p -> tarjetas.add(tarjeta(p)));
        if (pendientes.isEmpty()) mostrarVacio();
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private void mostrarVacio() {
        JPanel vacio = new JPanel(new FlujoConSalto());
        vacio.add(Componentes.nota("No hay nada esperando. Cuando el importador traiga títulos nuevos van a "
                + "aparecer acá."));
        JButton importar = new JButton("Traer cartelera ahora");
        importar.addActionListener(e -> navegacion.ir(Destino.IMPORTADOR));
        vacio.add(importar);
        tarjetas.add(vacio);
    }

    // Saca solo la decidida: recargar todo volvería a bajar cada póster.
    private void sacar(JPanel tarjeta) {
        tarjetas.remove(tarjeta);
        if (tarjetas.getComponentCount() == 0) mostrarVacio();
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private JPanel tarjeta(Pelicula pelicula) {
        // Sin ancho fijo: corta línea al de la tarjeta, que es todo lo que deja el póster.
        JLabel datos = Componentes.texto("<b style='font-size:13pt'>"
                + escapar(pelicula.titulo()) + "</b><br>"
                + (pelicula.anio() > 0 ? pelicula.anio() : "—") + " · " + duracion(pelicula.duracionMinutos())
                + " · " + etiqueta(pelicula.clasificacion()) + "<br>"
                + escapar(vacioSi(pelicula.director(), "Sin director")) + "<br>"
                + pelicula.generos().stream().map(g -> etiqueta(g)).collect(Collectors.joining(", "))
                + "<p style='margin-top:6px'>" + escapar(vacioSi(pelicula.sinopsis(), "Sin sinopsis.")) + "</p>");
        JButton confirmar = new JButton("Confirmar");
        JButton descartar = new JButton("Descartar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(descartar);
        botones.add(confirmar);
        JPanel contenido = new JPanel(new BorderLayout(16, 8));
        JPanel tarjeta = Componentes.conBorde(contenido);
        // Deshabilitados mientras viaja: dos clics seguidos mandarían confirmar y descartar la misma película.
        confirmar.addActionListener(e -> decidir(tarjeta, confirmar, descartar,
                () -> apiCartelera.confirmarPelicula(pelicula.id()),
                pelicula.titulo() + " confirmada: ya se puede programar"));
        descartar.addActionListener(e -> {
            // No tiene vuelta atrás: el importador no la vuelve a proponer.
            if (!confirmar("¿Descartar " + pelicula.titulo() + "? El importador no la vuelve a proponer.",
                    "Sí, descartar")) return;
            decidir(tarjeta, confirmar, descartar, () -> apiCartelera.descartarPelicula(pelicula.id()),
                    pelicula.titulo() + " descartada");
        });

        JPanel texto = new JPanel(new BorderLayout(0, 8));
        texto.add(datos, BorderLayout.NORTH);
        texto.add(botones, BorderLayout.SOUTH);
        JPanel poster = new JPanel(new BorderLayout());
        poster.add(new Poster(apiImagenes, pelicula.posterUrl()), BorderLayout.NORTH);
        contenido.add(poster, BorderLayout.WEST);
        contenido.add(texto, BorderLayout.CENTER);
        return tarjeta;
    }

    private void decidir(JPanel tarjeta, JButton confirmar, JButton descartar, Callable<Pelicula> pedido,
                         String mensaje) {
        confirmar.setEnabled(false);
        descartar.setEnabled(false);
        Tarea.ejecutar(this, pedido, hecha -> {
            avisar(mensaje);
            sacar(tarjeta);
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
