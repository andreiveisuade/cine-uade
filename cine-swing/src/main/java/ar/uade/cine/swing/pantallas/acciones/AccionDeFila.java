package ar.uade.cine.swing.pantallas.acciones;

import java.util.Optional;

// Una operación sobre la fila elegida de un listado: qué dice su botón, qué pregunta, qué pide y qué avisa.
// Patrón Command: ConcreteCommand por operación, con su Api (Receiver); BotonesDeFila (Invoker) la dispara sin saber cuál es.
public interface AccionDeFila<T> {

    /** El texto del botón mientras no hay fila elegida. */
    String texto();

    /** El de una fila: una acción que alterna dice cuál de las dos va a hacer ("Dar de baja" o "Reactivar"). */
    default String texto(T fila) {
        return texto();
    }

    default boolean habilitada(T fila) {
        return true;
    }

    /** Lo destructivo o sin vuelta atrás devuelve qué preguntar antes de mandar (ver Mensajes); lo demás, nada. */
    default Optional<Pregunta> pregunta(T fila) {
        return Optional.empty();
    }

    /** El pedido al backend. Corre fuera del EDT. */
    void ejecutar(T fila);

    /** Lo que se avisa si salió bien. Se arma con la fila como estaba antes del pedido. */
    String hecho(T fila);
}
