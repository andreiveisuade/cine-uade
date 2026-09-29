package ar.uade.cine.model.cartelera;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

// Registro de una corrida del importador de TMDB; Experto: sabe si terminó, falló o quedó colgada.
@Entity
@Getter
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

    public boolean estaEnCurso() {
        return estado == EstadoImportacion.EN_CURSO;
    }

    public boolean quedoColgada(Duration plazo, LocalDateTime ahora) {
        return estaEnCurso() && pedidaEn.plus(plazo).isBefore(ahora);
    }

    public boolean terminoHaceMenosDe(Duration espera, LocalDateTime ahora) {
        return terminoEn != null && terminoEn.plus(espera).isAfter(ahora);
    }
}
