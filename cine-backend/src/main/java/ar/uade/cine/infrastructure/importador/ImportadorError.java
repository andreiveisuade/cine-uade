package ar.uade.cine.infrastructure.importador;

public class ImportadorError extends RuntimeException {

    public ImportadorError(String mensaje) {
        super(mensaje);
    }

    public ImportadorError(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
