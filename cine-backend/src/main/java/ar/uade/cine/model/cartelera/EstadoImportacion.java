package ar.uade.cine.model.cartelera;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Estado de una corrida del importador; EN_CURSO hace de candado y no deja arrancar otra.
// Patrón State (GoF), como EstadoReserva y EstadoRevision: el comportamiento que depende del estado vive en el estado.
// Qué resolvía: Importacion preguntaba `estado == EN_CURSO` y sus transiciones no estaban escritas en ningún lado,
// así que terminar una corrida que ya había fallado pasaba sin aviso.
// Cómo se lee: EN_CURSO es la única que sobrescribe las transiciones; lo que no sobrescribe cae en los métodos
// de abajo, que rechazan. Una transición devuelve el estado siguiente e Importacion lo asigna.
public enum EstadoImportacion {

    EN_CURSO {
        @Override
        EstadoImportacion terminar() {
            return TERMINADA;
        }

        @Override
        EstadoImportacion fallar() {
            return FALLIDA;
        }

        @Override
        public boolean estaEnCurso() {
            return true;
        }
    },

    TERMINADA,

    FALLIDA;

    // Las transiciones son de paquete: solo Importacion cambia su estado.
    EstadoImportacion terminar() {
        throw new DatoInvalido("La importación ya terminó");
    }

    EstadoImportacion fallar() {
        throw new DatoInvalido("La importación ya terminó");
    }

    public boolean estaEnCurso() {
        return false;
    }
}
