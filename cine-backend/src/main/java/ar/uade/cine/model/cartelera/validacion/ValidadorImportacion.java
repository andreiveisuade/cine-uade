package ar.uade.cine.model.cartelera.validacion;

import ar.uade.cine.model.validacion.Regla;

// Valida lo que se le pide a una corrida del importador de TMDB; guarda de Regla que llama Importacion.
public final class ValidadorImportacion {

    // Cada página son veinte títulos y la corrida contesta recién al terminar: el tope acota la espera.
    private static final int PAGINAS_MAXIMAS = 3;

    private ValidadorImportacion() {
    }

    public static int paginas(int paginas) {
        return Regla.numero(paginas).entre(1, PAGINAS_MAXIMAS,
                "Las páginas a importar tienen que estar entre 1 y " + PAGINAS_MAXIMAS).valor();
    }
}
