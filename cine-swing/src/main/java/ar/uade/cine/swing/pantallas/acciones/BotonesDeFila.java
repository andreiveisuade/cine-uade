package ar.uade.cine.swing.pantallas.acciones;

import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
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
        var elegida = tabla.seleccionada();
        for (Boton<T> b : botones) {
            b.boton().setEnabled(elegida.map(b.accion()::habilitada).orElse(false));
            b.boton().setText(elegida.map(b.accion()::texto).orElse(b.accion().texto()));
        }
    }

    private void disparar(AccionDeFila<T> accion) {
        tabla.seleccionada().ifPresent(fila -> {
            if (accion.pregunta(fila).map(p -> !confirmar.test(p)).orElse(false)) return;
            String hecho = accion.hecho(fila);
            Tarea.ejecutar(origen, () -> {
                accion.ejecutar(fila);
                return null;
            }, resultado -> {
                Mensajes.exito(origen, hecho);
                alTerminar.run();
            });
        });
    }
}
