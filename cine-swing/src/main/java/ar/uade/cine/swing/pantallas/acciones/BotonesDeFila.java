package ar.uade.cine.swing.pantallas.acciones;

import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Los botones de las acciones de fila: cada uno toma la fila elegida, confirma si hace falta, pide y recarga.
// Invoker del Command: acá se cumple "lo destructivo se confirma" (Mensajes), para todas las acciones.
public final class BotonesDeFila<T> {

    private record Boton<T>(AccionDeFila<T> accion, JButton boton) {
    }

    private final Component origen;
    private final Tabla<T> tabla;
    private final Runnable alTerminar;
    private final Predicate<Pregunta> confirmar;
    private final List<Boton<T>> botones;

    /** {@code origen}: la pantalla, para el cursor de espera y el aviso. {@code alTerminar}: casi siempre recargar. */
    @SafeVarargs
    public BotonesDeFila(Component origen, Tabla<T> tabla, Runnable alTerminar, AccionDeFila<T>... acciones) {
        this(origen, tabla, alTerminar, p -> Mensajes.confirmar(origen, p.texto(), p.si(), p.no()), List.of(acciones));
    }

    // Con la confirmación aparte: en headless un diálogo no se puede abrir, y los tests contestan por el encargado.
    BotonesDeFila(Component origen, Tabla<T> tabla, Runnable alTerminar, Predicate<Pregunta> confirmar,
                  List<AccionDeFila<T>> acciones) {
        this.origen = origen;
        this.tabla = tabla;
        this.alTerminar = alTerminar;
        this.confirmar = confirmar;
        this.botones = acciones.stream().map(a -> new Boton<>(a, boton(a))).toList();
        botones.forEach(b -> b.boton().addActionListener(e -> disparar(b.accion())));
    }

    private static JButton boton(AccionDeFila<?> accion) {
        JButton boton = new JButton(accion.texto());
        return accion.destructiva() ? Componentes.peligro(boton) : boton;
    }

    /** Después de los botones propios de la pantalla (Editar, Cobrar): no le piden nada al backend, no son Command. */
    public void agregarA(JPanel panel) {
        botones.forEach(b -> Componentes.agregarALaDerecha(panel, b.boton()));
    }

    /** Texto y habilitación según la fila elegida; sin fila, apagados. La pantalla lo llama al cambiar la fila. */
    public void refrescar() {
        List<T> elegidas = tabla.seleccionadas();
        for (Boton<T> b : botones) {
            b.boton().setEnabled(elegidas.stream().anyMatch(b.accion()::habilitada));
            b.boton().setText(elegidas.size() == 1 ? b.accion().texto(elegidas.get(0)) : b.accion().texto());
        }
    }

    private void disparar(AccionDeFila<T> accion) {
        List<T> filas = tabla.seleccionadas().stream().filter(accion::habilitada).toList();
        if (filas.size() == 1) {
            dispararUna(accion, filas.get(0));
        } else if (filas.size() > 1) {
            dispararVarias(accion, filas);
        }
    }

    private void dispararUna(AccionDeFila<T> accion, T fila) {
        if (accion.pregunta(fila).map(p -> !confirmar.test(p)).orElse(false)) return;
        String hecho = accion.hecho(fila);
        Tarea.ejecutar(origen, () -> {
            accion.ejecutar(fila);
            return null;
        }, resultado -> {
            Mensajes.exito(origen, hecho);
            alTerminar.run();
        });
    }

    // Una por una: lo que el backend rechaza no frena al resto, y su motivo se muestra tal cual al terminar.
    private void dispararVarias(AccionDeFila<T> accion, List<T> filas) {
        String verbo = accion.texto();
        boolean pregunta = accion.pregunta(filas.get(0)).isPresent();
        if (pregunta && !confirmar.test(new Pregunta("¿" + verbo + " " + filas.size() + " elegidas?",
                "Sí, " + verbo.toLowerCase()))) return;
        Tarea.ejecutar(origen, () -> {
            List<String> rechazos = new ArrayList<>();
            for (T fila : filas) {
                try {
                    accion.ejecutar(fila);
                } catch (ErrorApi e) {
                    // Sin sesión las demás fallarían igual, y la app ya vuelve al ingreso.
                    if (e.esSesionVencida()) throw e;
                    rechazos.add(e.getMessage());
                }
            }
            return rechazos;
        }, rechazos -> {
            Mensajes.exito(origen, verbo + ": " + (filas.size() - rechazos.size()) + " hechas"
                    + (rechazos.isEmpty() ? "" : ", " + rechazos.size() + " no se pudieron"));
            if (!rechazos.isEmpty()) Mensajes.error(origen, String.join("\n", rechazos.stream().limit(10).toList()));
            alTerminar.run();
        });
    }
}
