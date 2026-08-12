package ar.uade.cine.interfaces;

import ar.uade.cine.modelo.TipoAsiento;

/**
 * Butaca física de una sala. No sabe si está ocupada: eso depende de la función,
 * porque la misma butaca puede estar tomada a las 20:00 y libre a las 22:30.
 */
public interface Asiento {

    int getId();

    void setId(int id);

    int getSalaId();

    /** 1 = fila A, 2 = fila B, y así. */
    int getFila();

    int getNumero();

    TipoAsiento getTipo();

    /** Identificación legible: "B7". */
    String getCodigo();
}
