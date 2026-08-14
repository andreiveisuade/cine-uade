package ar.uade.cine.importador;

/**
 * No se pudo correr la importación.
 *
 * <p>El mensaje se le muestra al encargado tal cual, así que tiene que decirle qué hacer:
 * «el importador no responde en http://parser:8090: ¿está levantado?» sirve, «connection
 * refused» no.
 *
 * <p>Es de ejecución y no verificada porque no hay nada que el gestor pueda hacer para
 * evitarla —el importador está o no está— y porque el que la termina manejando es uno
 * solo: {@code GestorImportaciones}, que la convierte en una importación FALLIDA.
 */
public class ImportadorError extends RuntimeException {

    public ImportadorError(String mensaje) {
        super(mensaje);
    }

    public ImportadorError(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
