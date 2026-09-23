package ar.uade.cine.infrastructure.importador;

/**
 * No se pudo correr la importación. El mensaje se le muestra al encargado tal cual, así que
 * tiene que decirle qué hacer. {@code GestorImportaciones} la convierte en importación FALLIDA.
 */
public class ImportadorError extends RuntimeException {

    public ImportadorError(String mensaje) {
        super(mensaje);
    }

    public ImportadorError(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
