package ar.uade.cine.dominio.salas;

/**
 * Estado físico de la butaca: si está en condiciones de venderse o rota. No dice si
 * está ocupada —eso depende de la función—, sino si el cine la puede ofrecer: una
 * butaca rota lo está para todas las funciones.
 */
public enum EstadoAsiento {
    HABILITADO,
    FUERA_DE_SERVICIO
}
