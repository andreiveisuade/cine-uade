package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Asiento;

public class AsientoImpl implements Asiento {

    private int id;
    private int salaId;
    private int fila;
    private int numero;
    private TipoAsiento tipo;
    private EstadoAsiento estado;

    public AsientoImpl(int salaId, int fila, int numero, TipoAsiento tipo) {
        this.salaId = salaId;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
        this.estado = EstadoAsiento.DISPONIBLE;
    }

    public AsientoImpl(int id, int salaId, int fila, int numero, TipoAsiento tipo, EstadoAsiento estado) {
        this(salaId, fila, numero, tipo);
        this.id = id;
        this.estado = estado;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getSalaId() {
        return salaId;
    }

    @Override
    public int getFila() {
        return fila;
    }

    @Override
    public int getNumero() {
        return numero;
    }

    @Override
    public TipoAsiento getTipo() {
        return tipo;
    }

    @Override
    public EstadoAsiento getEstado() {
        return estado;
    }

    @Override
    public void setEstado(EstadoAsiento estado) {
        this.estado = estado;
    }

    @Override
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
