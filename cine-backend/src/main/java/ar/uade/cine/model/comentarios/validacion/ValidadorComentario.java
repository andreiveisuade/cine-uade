package ar.uade.cine.model.comentarios.validacion;

import ar.uade.cine.model.validacion.Regla;

// Valida el puntaje y la reseña de un comentario; Pure Fabrication que llama Comentario.
public final class ValidadorComentario {

    // VARCHAR(1000) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO = 1000;

    private ValidadorComentario() {
    }

    public static int puntaje(Integer puntaje) {
        return Regla.numero(puntaje).obligatorio("Falta el puntaje")
                .entre(1, 5, "El puntaje tiene que estar entre 1 y 5").valor();
    }

    public static String texto(String texto) {
        return Regla.texto(texto).obligatorio("Falta la reseña").recortado()
                .hasta(LARGO_MAXIMO, "La reseña").valor();
    }
}
