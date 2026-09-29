package ar.uade.cine.model.rechazos;

// Base de lo que el sistema rechaza con un texto para el usuario; jerarquía sellada, un status por tipo.
// Extiende IllegalArgumentException para que los assertThrows(IllegalArgumentException.class, …) de los
// tests sigan valiendo: un rechazo es un argumento que no se acepta. Pero no toda IllegalArgumentException
// es un rechazo: la de una librería trae un texto técnico, y ManejadorErrores la contesta con 500.
// Sellada para que la lista de tipos sea cerrada: uno nuevo se suma acá, y ManejadorErroresTest falla
// hasta que ManejadorErrores le dé su status.
public abstract sealed class Rechazo extends IllegalArgumentException
        permits DatoInvalido, RecursoNoEncontrado, ConflictoDeNegocio, ButacaOcupada {

    protected Rechazo(String mensaje) {
        super(mensaje);
    }

    protected Rechazo(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
