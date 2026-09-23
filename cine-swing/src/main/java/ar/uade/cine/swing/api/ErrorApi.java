package ar.uade.cine.swing.api;

// Unchecked para que las pantallas no tengan que declararla en cada lambda de SwingWorker.
// `estado` 0 es que ni se llegó al servidor; si no, el código HTTP (409 es la carrera por la butaca, distinta del 400).
public class ErrorApi extends RuntimeException {

    private final int estado;

    public ErrorApi(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int estado() {
        return estado;
    }

    public boolean esSesionVencida() {
        return estado == 401;
    }
}
