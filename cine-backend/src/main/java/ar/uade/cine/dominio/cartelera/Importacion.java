package ar.uade.cine.dominio.cartelera;

import java.time.LocalDateTime;

/**
 * Una corrida del importador de cartelera: cuándo se pidió, cómo terminó y qué trajo.
 *
 * <p>Es una entidad y no un dato de infraestructura, y por eso se guarda. La pregunta que
 * responde —«¿cuándo fue la última vez que trajimos cartelera, y cuánto entró?»— es del
 * encargado un lunes a la mañana, no del que despliega: si viviera en memoria, un reinicio
 * del backend la borraría y la pantalla no tendría nada que mostrar.
 *
 * <p>No guarda <em>qué</em> películas entraron, solo cuántas. Las películas ya están
 * guardadas, con su {@link EstadoRevision}, y son las que se ven en el buzón: repetir acá
 * la lista sería tener dos fuentes de verdad para lo mismo y que un día no coincidan.
 * {@link #getDetalle()} es un texto para leer, no un dato para consultar.
 */
public interface Importacion {

    int getId();

    void setId(int id);

    /**
     * Cuántas páginas de TMDB se pidieron, de veinte títulos cada una. Es lo único que el
     * encargado elige, y queda registrado porque explica los contadores: una corrida de
     * tres páginas que trae dos películas nuevas no dice lo mismo que una de una.
     */
    int getPaginas();

    LocalDateTime getPedidaEn();

    /** Cuándo volvió el importador, o {@code null} si todavía está en curso. */
    LocalDateTime getTerminoEn();

    EstadoImportacion getEstado();

    /** Las que entraron al buzón en esta corrida. */
    int getNuevas();

    /** Las que ya estaban en el catálogo, o que TMDB trajo sin duración y no sirven. */
    int getSalteadas();

    /** Las que el backend rechazó por una regla, o que TMDB no pudo completar. */
    int getFallidas();

    /**
     * El texto para mirar cuando los números no alcanzan: el log de la corrida si terminó
     * bien, el motivo si falló. Puede ser {@code null}.
     */
    String getDetalle();

    /**
     * Cierra la corrida con lo que respondió el importador.
     *
     * <p>Los cuatro campos se ponen juntos a propósito: son un solo hecho, y dejar que el
     * gestor los setee de a uno es la forma de que un día quede una fila TERMINADA sin
     * fecha de fin.
     */
    void terminar(int nuevas, int salteadas, int fallidas, String detalle, LocalDateTime cuando);

    /** La cierra como fallida, con el mensaje que se le va a mostrar al encargado. */
    void fallar(String motivo, LocalDateTime cuando);
}
