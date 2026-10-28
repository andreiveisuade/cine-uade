package ar.uade.cine.model.rechazos;

// Base de lo que el sistema rechaza con un texto para el usuario; jerarquía sellada, un status por tipo.
// No IllegalArgumentException: Spring Data la traduce al salir de un repositorio y el 404 saldría 500.
// Sellada: un tipo nuevo hace fallar ManejadorErroresTest hasta que ManejadorErrores le dé su status.
public abstract sealed class Rechazo extends RuntimeException
        permits DatoInvalido, RecursoNoEncontrado, ConflictoDeNegocio, ButacaOcupada,
        DemasiadosIntentos {

    protected Rechazo(String mensaje) {
        super(mensaje);
    }

    protected Rechazo(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
