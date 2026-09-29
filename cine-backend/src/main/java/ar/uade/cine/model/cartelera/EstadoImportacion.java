package ar.uade.cine.model.cartelera;

// Estado de una corrida del importador; EN_CURSO hace de candado y no deja arrancar otra.
public enum EstadoImportacion {

    EN_CURSO,

    TERMINADA,

    FALLIDA
}
