package ar.uade.cine.model.salas;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    public int getSalaId() {
        return salaId;
    }

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

    public String getCodigo() {
        return codigoDe(fila, numero);
    }

    public static String codigoDe(int fila, int numero) {
        return (char) ('A' + fila - 1) + String.valueOf(numero);
    }

    public static Optional<Asiento> conCodigo(List<Asiento> asientos, String codigo) {
        String buscado = normalizarCodigo(codigo);
        return asientos.stream().filter(a -> a.getCodigo().equals(buscado)).findFirst();
    }

    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
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
