package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Poster;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.duracion;
import static ar.uade.cine.swing.comun.Formato.escapar;

// El buzón de lo que trajo el importador: hasta que se confirma, no se programa ni lo ve el cliente.
public final class PantallaPendientes extends Pantalla {

    private record Fila(Pelicula pelicula, JPanel tarjeta, JCheckBox elegida, JButton confirmar, JButton descartar) {

        // Deshabilitada mientras viaja: dos clics seguidos mandarían confirmar y descartar la misma película.
        void habilitar(boolean si) {
            elegida.setEnabled(si);
            confirmar.setEnabled(si);
            descartar.setEnabled(si);
        }
    }

    private record Lote(List<Fila> hechas, List<String> rechazos) {
    }

    private final ApiCartelera apiCartelera;
    private final ApiImagenes apiImagenes;
    private final Navegacion navegacion;
    private final Predicate<String> confirmaLote;
    private final Consumer<String> muestraRechazos;
    private final JPanel tarjetas = new JPanel(new GridLayout(0, 1, 0, Escala.CERCA));
    private final List<Fila> filas = new ArrayList<>();
    private final JCheckBox todas = new JCheckBox("Seleccionar todas");
    private final JLabel cuantas = new JLabel(" ");
    private final JButton confirmarElegidas = Componentes.primario(new JButton("Confirmar elegidas"));
    private final JButton descartarElegidas = Componentes.peligro(new JButton("Descartar elegidas"));
    private final JPanel barraDeLote = new JPanel(new BorderLayout(Escala.BLOQUE, 0));

    public PantallaPendientes(ApiCartelera apiCartelera, ApiImagenes apiImagenes, Navegacion navegacion) {
        this(apiCartelera, apiImagenes, navegacion, null, null);
    }

    // Los diálogos del lote entran por acá para poder probarlo sin ventana; null es el diálogo de verdad.
    PantallaPendientes(ApiCartelera apiCartelera, ApiImagenes apiImagenes, Navegacion navegacion,
                       Predicate<String> confirmaLote, Consumer<String> muestraRechazos) {
        super("Por revisar", "Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no "
                + "se pueden programar ni las ve el cliente. Lo que descartes queda descartado: el importador no lo "
                + "vuelve a proponer.");
        this.apiCartelera = apiCartelera;
        this.apiImagenes = apiImagenes;
        this.navegacion = navegacion;
        this.confirmaLote = confirmaLote != null ? confirmaLote : pregunta -> confirmar(pregunta, "Sí, descartar");
        this.muestraRechazos = muestraRechazos != null ? muestraRechazos : texto -> Mensajes.error(this, texto);

        JPanel arriba = new AlAnchoDelVisor(new BorderLayout(), false);
        arriba.add(tarjetas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(arriba);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel izquierda = new JPanel(new BorderLayout(Escala.APARTE, 0));
        izquierda.add(todas, BorderLayout.WEST);
        izquierda.add(cuantas, BorderLayout.CENTER);
        barraDeLote.add(izquierda, BorderLayout.WEST);
        barraDeLote.add(Componentes.aLaDerecha(descartarElegidas, confirmarElegidas), BorderLayout.EAST);
        barraDeLote.setVisible(false);
        todas.addActionListener(e -> {
            filas.stream().filter(f -> f.elegida().isEnabled()).forEach(f -> f.elegida().setSelected(todas.isSelected()));
            actualizarLote();
        });
        confirmarElegidas.addActionListener(e -> decidirElegidas(apiCartelera::confirmarPelicula, "Confirmadas"));
        descartarElegidas.addActionListener(e -> {
            // No tiene vuelta atrás: el importador no las vuelve a proponer.
            if (!this.confirmaLote.test("¿Descartar las elegidas (" + elegidas().size()
                    + ")? El importador no las vuelve a proponer.")) return;
            decidirElegidas(apiCartelera::descartarPelicula, "Descartadas");
        });
        JPanel cuerpo = new JPanel(new BorderLayout(0, Escala.CERCA));
        cuerpo.add(barraDeLote, BorderLayout.NORTH);
        cuerpo.add(scroll, BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);
        recargar();
    }

    private void recargar() {
        cargar(apiCartelera::obtenerPeliculasPendientes, this::pintar);
    }

    private void pintar(List<Pelicula> pendientes) {
        tarjetas.removeAll();
        filas.clear();
        pendientes.forEach(p -> tarjetas.add(tarjeta(p)));
        if (pendientes.isEmpty()) mostrarVacio();
        actualizarLote();
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private void mostrarVacio() {
        JPanel vacio = new JPanel(new FlujoConSalto());
        vacio.add(Componentes.nota("No hay nada esperando. Cuando el importador traiga títulos nuevos van a "
                + "aparecer acá."));
        JButton importar = Componentes.primario(new JButton("Traer cartelera ahora"));
        importar.addActionListener(e -> navegacion.ir(Destino.IMPORTADOR));
        vacio.add(importar);
        tarjetas.add(vacio);
    }

    // Saca solo la decidida: recargar todo volvería a bajar cada póster.
    private void sacar(Fila fila) {
        filas.remove(fila);
        tarjetas.remove(fila.tarjeta());
        if (tarjetas.getComponentCount() == 0) mostrarVacio();
        actualizarLote();
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private JPanel tarjeta(Pelicula pelicula) {
        // Título, datos y la decisión en una línea; la sinopsis debajo corta al ancho que deja el póster.
        List<String> datos = new ArrayList<>();
        if (pelicula.anio() > 0) datos.add(String.valueOf(pelicula.anio()));
        datos.add(duracion(pelicula.duracionMinutos()));
        datos.add(etiqueta(pelicula.clasificacion()));
        if (pelicula.director() != null && !pelicula.director().isBlank()) datos.add(escapar(pelicula.director()));
        datos.add(pelicula.generos().stream().map(g -> etiqueta(g)).collect(Collectors.joining(", ")));
        JButton confirmar = Componentes.primario(new JButton("Confirmar"));
        JButton descartar = Componentes.peligro(new JButton("Descartar"));
        // El título es la casilla: toda la línea sirve para tildar, sin apuntarle a un cuadradito.
        JCheckBox elegida = new JCheckBox(pelicula.titulo());
        elegida.setFont(Componentes.subtitulo(pelicula.titulo()).getFont());
        elegida.addActionListener(e -> actualizarLote());
        JPanel cabeza = new JPanel(new BorderLayout(Escala.BLOQUE, 0));
        cabeza.add(new Pila().agregar(elegida,
                Componentes.nota(String.join(" · ", datos))), BorderLayout.CENTER);
        cabeza.add(Componentes.aLaDerecha(descartar, confirmar), BorderLayout.EAST);
        JPanel contenido = new JPanel(new BorderLayout(Escala.APARTE, 0));
        JPanel tarjeta = Componentes.conBorde(contenido);
        Fila fila = new Fila(pelicula, tarjeta, elegida, confirmar, descartar);
        filas.add(fila);
        confirmar.addActionListener(e -> decidir(fila,
                () -> apiCartelera.confirmarPelicula(pelicula.id()),
                pelicula.titulo() + " confirmada: ya se puede programar"));
        descartar.addActionListener(e -> {
            // No tiene vuelta atrás: el importador no la vuelve a proponer.
            if (!confirmar("¿Descartar " + pelicula.titulo() + "? El importador no la vuelve a proponer.",
                    "Sí, descartar")) return;
            decidir(fila, () -> apiCartelera.descartarPelicula(pelicula.id()),
                    pelicula.titulo() + " descartada");
        });

        JPanel texto = new JPanel(new BorderLayout(0, Escala.CERCA));
        texto.add(cabeza, BorderLayout.NORTH);
        texto.add(Componentes.texto(escapar(vacioSi(pelicula.sinopsis(), "Sin sinopsis"))), BorderLayout.CENTER);
        contenido.add(Componentes.arriba(new Poster(apiImagenes, pelicula.posterUrl())), BorderLayout.WEST);
        contenido.add(texto, BorderLayout.CENTER);
        return tarjeta;
    }

    private void decidir(Fila fila, Callable<Pelicula> pedido, String mensaje) {
        fila.habilitar(false);
        actualizarLote();
        Tarea.ejecutar(this, pedido, hecha -> {
            avisar(mensaje);
            sacar(fila);
        }, error -> {
            fila.habilitar(true);
            actualizarLote();
            Mensajes.error(this, error);
        });
    }

    private List<Fila> elegidas() {
        return filas.stream().filter(f -> f.elegida().isSelected() && f.elegida().isEnabled()).toList();
    }

    // De a una y sin cortar: la que el backend rechaza queda en la lista, tildada, y las demás siguen.
    private void decidirElegidas(IntFunction<Pelicula> pedido, String hechas) {
        List<Fila> lote = elegidas();
        lote.forEach(f -> f.habilitar(false));
        actualizarLote();
        Tarea.ejecutar(this, () -> {
            List<Fila> bien = new ArrayList<>();
            List<String> rechazos = new ArrayList<>();
            for (Fila fila : lote) {
                try {
                    pedido.apply(fila.pelicula().id());
                    bien.add(fila);
                } catch (ErrorApi e) {
                    // Sin sesión las demás fallarían igual, y la app ya vuelve al ingreso.
                    if (e.esSesionVencida()) throw e;
                    rechazos.add(fila.pelicula().titulo() + ": " + e.getMessage());
                }
            }
            return new Lote(bien, rechazos);
        }, resultado -> {
            resultado.hechas().forEach(this::sacar);
            lote.stream().filter(filas::contains).forEach(f -> f.habilitar(true));
            actualizarLote();
            avisar(hechas + ": " + resultado.hechas().size()
                    + (resultado.rechazos().isEmpty() ? "" : ", no se pudieron: " + resultado.rechazos().size()));
            if (!resultado.rechazos().isEmpty()) {
                muestraRechazos.accept(String.join("\n", resultado.rechazos().stream().limit(10).toList()));
            }
        });
    }

    private void actualizarLote() {
        int elegidas = elegidas().size();
        barraDeLote.setVisible(!filas.isEmpty());
        todas.setSelected(elegidas > 0 && elegidas == filas.size());
        cuantas.setText("Elegidas: " + elegidas + " de " + filas.size());
        confirmarElegidas.setEnabled(elegidas > 0);
        descartarElegidas.setEnabled(elegidas > 0);
    }

    private static String vacioSi(String valor, String reemplazo) {
        return valor == null || valor.isBlank() ? reemplazo : valor;
    }
}
