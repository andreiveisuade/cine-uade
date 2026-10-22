package ar.uade.cine.model.rechazos;

// Muchas pruebas fallidas seguidas del mismo email o IP (login, recuperación R25); se responde 429.
// El texto es uno solo: lo da también el filtro del login, antes de llegar a un controller.
public final class DemasiadosIntentos extends Rechazo {

    public static final String MENSAJE = "Demasiados intentos: esperá un minuto y probá de nuevo";

    public DemasiadosIntentos() {
        super(MENSAJE);
    }
}
