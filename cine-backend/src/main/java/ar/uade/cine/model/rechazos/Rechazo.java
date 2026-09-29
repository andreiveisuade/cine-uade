package ar.uade.cine.model.rechazos;

// Base de lo que el sistema rechaza con un texto para el usuario; jerarquía sellada, un status por tipo.
// Extiende RuntimeException y no IllegalArgumentException: un rechazo es una regla del cine, no un error
// de programación. Además Spring Data traduce la IllegalArgumentException que sale de un repositorio a
// InvalidDataAccessApiUsageException, y el 404 de Repositorio.exigir se habría convertido en un 500.
// Sellada para que la lista de tipos sea cerrada: uno nuevo se suma acá, y ManejadorErroresTest falla
// hasta que ManejadorErrores le dé su status.
public abstract sealed class Rechazo extends RuntimeException
        permits DatoInvalido, RecursoNoEncontrado, ConflictoDeNegocio, ButacaOcupada {

    protected Rechazo(String mensaje) {
        super(mensaje);
    }

    protected Rechazo(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
