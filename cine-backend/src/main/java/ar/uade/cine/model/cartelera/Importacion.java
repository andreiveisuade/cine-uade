package ar.uade.cine.model.cartelera;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
 *
 * <p>Nace EN_CURSO: una importación existe porque alguien la pidió y ya se está haciendo.
 * El momento entra por parámetro en vez de tomarlo de {@code LocalDateTime.now()} acá
 * adentro, que es lo que deja probar la caducidad de una corrida colgada sin esperar cinco
 * minutos de reloj.
 */
@Entity
public class Importacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int paginas;

    private LocalDateTime pedidaEn;

    @Enumerated(EnumType.STRING)
    private EstadoImportacion estado = EstadoImportacion.EN_CURSO;

    private LocalDateTime terminoEn;

    private int nuevas;

    private int salteadas;

    private int fallidas;

    /** El log de la corrida, o el motivo si falló: es texto para leer, no para consultar. */
    @Column(columnDefinition = "TEXT")
    private String detalle;

    protected Importacion() {
    }

    public Importacion(int paginas, LocalDateTime pedidaEn) {
        this.paginas = paginas;
        this.pedidaEn = pedidaEn;
    }

    public void terminar(int nuevas, int salteadas, int fallidas, String detalle,
                         LocalDateTime cuando) {
        this.estado = EstadoImportacion.TERMINADA;
        this.nuevas = nuevas;
        this.salteadas = salteadas;
        this.fallidas = fallidas;
        this.detalle = detalle;
        this.terminoEn = cuando;
    }

    public void fallar(String motivo, LocalDateTime cuando) {
        this.estado = EstadoImportacion.FALLIDA;
        this.detalle = motivo;
        this.terminoEn = cuando;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPaginas() {
        return paginas;
    }

    public LocalDateTime getPedidaEn() {
        return pedidaEn;
    }

    /** Null mientras la corrida sigue: es "todavía no terminó", no una fecha vacía. */
    public LocalDateTime getTerminoEn() {
        return terminoEn;
    }

    public EstadoImportacion getEstado() {
        return estado;
    }

    public int getNuevas() {
        return nuevas;
    }

    public int getSalteadas() {
        return salteadas;
    }

    public int getFallidas() {
        return fallidas;
    }

    public String getDetalle() {
        return detalle;
    }
}
