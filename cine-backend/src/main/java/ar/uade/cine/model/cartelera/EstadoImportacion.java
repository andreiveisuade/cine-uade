package ar.uade.cine.model.cartelera;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Estado de una corrida del importador; EN_CURSO hace de candado y no deja arrancar otra.
// Patrón State (GoF): el comportamiento que depende del estado vive en el estado.
// Solo EN_CURSO sobrescribe las transiciones; las demás rechazan. Devuelven el estado siguiente e Importacion lo asigna.
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
