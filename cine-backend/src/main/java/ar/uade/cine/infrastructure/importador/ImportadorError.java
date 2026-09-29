package ar.uade.cine.infrastructure.importador;

// Falla al hablar con el catálogo externo; GestorImportaciones la atrapa y la anota como corrida fallida.
public class ImportadorError extends RuntimeException {

    public ImportadorError(String mensaje) {
        super(mensaje);
    }

    public ImportadorError(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
