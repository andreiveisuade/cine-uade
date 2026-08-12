package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Asiento;

public class AsientoImpl implements Asiento {

    private int id;
    private int salaId;
    private int fila;
    private int numero;
    private TipoAsiento tipo;

    public AsientoImpl(int salaId, int fila, int numero, TipoAsiento tipo) {
        this.salaId = salaId;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
    }

    public AsientoImpl(int id, int salaId, int fila, int numero, TipoAsiento tipo) {
        this(salaId, fila, numero, tipo);
        this.id = id;
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
    public String getCodigo() {
        return (char) ('A' + fila - 1) + String.valueOf(numero);
    }

    @Override
    public String toString() {
        return getCodigo() + (tipo == TipoAsiento.ESTANDAR ? "" : " (" + tipo + ")");
    }
}
