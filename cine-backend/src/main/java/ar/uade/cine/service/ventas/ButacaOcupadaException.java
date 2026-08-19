package ar.uade.cine.service.ventas;

/**
 * Otra reserva se quedó con la butaca entre que la vimos libre y la quisimos guardar.
 *
 * <p>No es una falla técnica: es una carrera perdida, y quien la reciba tiene que poder
 * distinguirla para reaccionar distinto. El front vuelve a pedir el mapa de la sala y
 * repinta; la API contesta 409 y no 500.
 *
 * <p>Vive en {@code service/} y ya no en la capa de datos porque el que la levanta es el
 * gestor. Antes la construía el DAO al mirar el SQLState de MySQL; ahora Spring Data traduce
 * la violación del UNIQUE a una {@code DataIntegrityViolationException} y el gestor la
 * convierte en esta, que es la que sabe qué se estaba intentando hacer y con qué mensaje
 * contarlo.
 */
public class ButacaOcupadaException extends RuntimeException {

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
