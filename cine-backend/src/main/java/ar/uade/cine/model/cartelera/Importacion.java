package ar.uade.cine.model.cartelera;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    public int getPaginas() {
        return paginas;
    }

    public LocalDateTime getPedidaEn() {
        return pedidaEn;
    }

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
