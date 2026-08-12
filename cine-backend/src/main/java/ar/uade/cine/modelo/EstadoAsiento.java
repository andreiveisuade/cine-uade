package ar.uade.cine.modelo;

/**
 * Estado físico de la butaca. A diferencia de "ocupada" —que depende de la función—,
 * esto sí le pertenece al asiento: una butaca rota lo está para todas las funciones.
 */
public enum EstadoAsiento {
    DISPONIBLE,
    FUERA_DE_SERVICIO
}
