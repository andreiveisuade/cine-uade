package ar.uade.cine.model.cartelera;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Revisión de una película importada; patrón State: cada estado decide si se publica, se programa y se ve.
//
// Qué es: el patrón State pone el comportamiento que depende del estado adentro del estado mismo.
// Pelicula no pregunta «¿estás confirmada? ¿estás descartada?»: le delega la decisión a su
// estadoRevision, y cada constante responde a su manera.
//
// Qué problema resolvía: las preguntas por el estado estaban repartidas y repetidas.
// Pelicula.ponerEnCartelera comparaba contra CONFIRMADA, y GestorFunciones comparaba contra DESCARTADA
// y después contra CONFIRMADA, cada uno con sus textos. Para saber qué permite una descartada había que
// buscar todos esos if, y un estado nuevo obligaba a encontrarlos a todos. Acá lo que permite cada
// estado se lee junto, y como los métodos son abstractos, una constante nueva no compila hasta decir
// qué hace en cada operación.
//
// Cómo se lee: cada constante tiene su cuerpo { ... } con su versión de los métodos abstractos.
// CONFIRMADA deja hacer todo; PENDIENTE rechaza pidiendo que la revisen; DESCARTADA rechaza diciendo
// que se descartó, porque ya no está en el buzón y pedirle al encargado que la revise lo mandaría a
// buscarla donde no está. Solo la confirmada se le muestra al público. Las transiciones
// (dejarPendiente, confirmar, descartar) quedan en Pelicula, que es la que cambia de estado.
// Es un enum y no una jerarquía de clases porque los estados no tienen datos propios, y JPA guarda el
// nombre de la constante en estado_revision como antes, sin ningún mapeo extra. Los métodos son de
// paquete: afuera se le habla a la película (pelicula.exigirProgramable()), no a su estado.
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
