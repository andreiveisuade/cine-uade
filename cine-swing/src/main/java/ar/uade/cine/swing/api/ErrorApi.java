package ar.uade.cine.swing.api;

import lombok.Getter;
import lombok.experimental.Accessors;

// Unchecked para que las pantallas no tengan que declararla en cada lambda de SwingWorker.
// `estado` 0 es que ni se llegó al servidor, -1 que falló algo de este lado (un archivo, un error de programa); si no,
// el código HTTP. Swing nunca reserva ni bloquea butacas: un 409 acá es un nombre o un título repetido, que se corrige en
// un campo, o una reserva que otro pedido cambió en el medio (@Version), que no. Cuál de los dos, lo decide Validacion.
public class ErrorApi extends RuntimeException {

    @Getter
    @Accessors(fluent = true)
    private final int estado;

    public ErrorApi(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public boolean esSesionVencida() {
        return estado == 401;
    }

    /**
     * Un rechazo de lo que se mandó (400, o un 404 de algo tipeado, como un email): se corrige en el formulario. El 409
     * depende de si su mensaje nombra un campo, y eso solo lo sabe {@code Validacion#esDelFormulario}.
     */
    public boolean esDelFormulario() {
        return estado == 400 || estado == 404;
    }

    public boolean esConflicto() {
        return estado == 409;
    }

    /** No se llegó al servidor, o el proxy contestó que el backend no está (reiniciando, caído). */
    public boolean esSinConexion() {
        return estado == 0 || estado == 502 || estado == 503 || estado == 504;
    }
}
