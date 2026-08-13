package ar.uade.cine.persistencia;

/**
 * Otra reserva se quedó con la butaca entre que la vimos libre y la quisimos guardar.
 *
 * <p>Existe como excepción propia y no como un {@link PersistenciaException} más porque no
 * es una falla técnica: es una carrera perdida, y quien la reciba tiene que poder
 * distinguirla para reaccionar distinto. El menú vuelve a mostrar el mapa de la sala; una
 * API REST devolvería 409 y no 500.
 *
 * <p>Hereda de PersistenciaException para que el código que ya la atrapa siga andando.
 */
public class ButacaOcupadaException extends PersistenciaException {

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
