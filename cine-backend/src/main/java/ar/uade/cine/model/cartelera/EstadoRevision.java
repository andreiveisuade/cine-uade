package ar.uade.cine.model.cartelera;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Revisión de una película importada; patrón State: cada estado decide si se publica, se programa y se ve.
// CONFIRMADA deja hacer todo; PENDIENTE pide revisión; DESCARTADA dice que se descartó (ya no está en el buzón).
// Enum y no jerarquía: sin datos propios, y JPA guarda el nombre igual. Métodos de paquete: afuera se habla con Pelicula.
public enum EstadoRevision {

    // Lo que trae el importador: espera en el buzón a que alguien la mire.
    PENDIENTE {
        @Override
        void exigirPublicable(String titulo) {
            throw new DatoInvalido("La película " + titulo
                    + " no está confirmada: revisala antes de publicarla");
        }

        @Override
        void exigirProgramable(String titulo) {
            throw new DatoInvalido("La película " + titulo
                    + " todavía no está confirmada: revisala antes de programarla");
        }

        @Override
        boolean seMuestraAlPublico() {
            return false;
        }
    },

    // Lo que cargó el encargado, o lo que confirmó del buzón: cargarlo ya es haberlo decidido.
    CONFIRMADA {
        @Override
        void exigirPublicable(String titulo) {
        }

        @Override
        void exigirProgramable(String titulo) {
        }

        @Override
        boolean seMuestraAlPublico() {
            return true;
        }
    },

    // Queda guardada para que el importador no la vuelva a proponer.
    DESCARTADA {
        @Override
        void exigirPublicable(String titulo) {
            throw new DatoInvalido("La película " + titulo + " está descartada: no se puede publicar");
        }

        @Override
        void exigirProgramable(String titulo) {
            throw new DatoInvalido("La película " + titulo + " está descartada: no se puede programar");
        }

        @Override
        boolean seMuestraAlPublico() {
            return false;
        }
    };

    // Publicar es levantar el veto de enCartelera: sin esto, el botón Publicar saltearía el buzón.
    abstract void exigirPublicable(String titulo);

    // Una función la ofrece al cliente: programar también saltearía el buzón.
    abstract void exigirProgramable(String titulo);

    // Quien no es del personal no ve lo que el encargado todavía no aprobó, ni lo que descartó.
    abstract boolean seMuestraAlPublico();
}
