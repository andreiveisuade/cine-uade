package ar.uade.cine.model.salas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Butaca física de una sala. No sabe si está ocupada: eso depende de la función, porque la
 * misma butaca puede estar tomada a las 20:00 y libre a las 22:30. Sí sabe si está rota,
 * porque eso no cambia entre funciones.
 *
 * <p>A la sala la referencia por id y no con un {@code @ManyToOne}. Es a propósito y no una
 * traducción a medias: el dominio ya estaba escrito así —"referencia por id, el repositorio
 * trae el objeto completo cuando hace falta"— y mantenerlo deja cada entidad cargando con
 * sus propios datos y nada más. Con la referencia al objeto, listar las butacas de una sala
 * arrastraría la sala en cada fila, y quien solo quiere pintar el mapa no necesita nada de
 * eso. Las relaciones sí se mapean donde el objeto es parte del agregado —las entradas de
 * una reserva, los ítems de una compra—, que es donde una cosa no existe sin la otra.
 */
@Entity
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "sala_id")
    private int salaId;

    private int fila;

    private int numero;

    @Enumerated(EnumType.STRING)
    private TipoAsiento tipo;

    @Enumerated(EnumType.STRING)
    private EstadoAsiento estado;

    protected Asiento() {
    }

    public Asiento(int salaId, int fila, int numero, TipoAsiento tipo) {
        this.salaId = salaId;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
        this.estado = EstadoAsiento.HABILITADO;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSalaId() {
        return salaId;
    }

    /** 1 = fila A, 2 = fila B, y así. */
    public int getFila() {
        return fila;
    }

    public int getNumero() {
        return numero;
    }

    public TipoAsiento getTipo() {
        return tipo;
    }

    public EstadoAsiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsiento estado) {
        this.estado = estado;
    }

    /** Identificación legible: "B7". Se deriva de la fila y el número, no se guarda. */
    public String getCodigo() {
        return (char) ('A' + fila - 1) + String.valueOf(numero);
    }

    @Override
    public String toString() {
        String extra = tipo == TipoAsiento.ESTANDAR ? "" : " (" + tipo + ")";
        if (estado == EstadoAsiento.FUERA_DE_SERVICIO) {
            extra += " FUERA DE SERVICIO";
        }
        return getCodigo() + extra;
    }
}
