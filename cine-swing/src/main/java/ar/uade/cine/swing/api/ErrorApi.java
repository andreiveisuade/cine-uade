package ar.uade.cine.swing.api;

import lombok.Getter;
import lombok.experimental.Accessors;

// Un pedido que falló, con su código HTTP o por qué no llegó; de eso sale si va en línea o en un diálogo.
// `estado`: 0 no se llegó al servidor, -1 falló algo de este lado, si no el código HTTP; unchecked para las lambdas.
// Un 409 se corrige en un campo (nombre repetido) o no (@Version); lo decide Validacion.
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

    /** Rechazo de lo mandado (400, o 404 de algo tipeado): se corrige en el formulario; el 409 depende del mensaje. */
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
