package ar.uade.cine.interfaces;

import ar.uade.cine.modelo.EstadoAsiento;
import ar.uade.cine.modelo.TipoAsiento;

/**
 * Butaca física de una sala. No sabe si está ocupada: eso depende de la función,
 * porque la misma butaca puede estar tomada a las 20:00 y libre a las 22:30.
 * Sí sabe si está rota, porque eso no cambia entre funciones.
 */
public interface Asiento {

    int getId();

    void setId(int id);

    int getSalaId();

    /** 1 = fila A, 2 = fila B, y así. */
    int getFila();

    int getNumero();

    TipoAsiento getTipo();

    EstadoAsiento getEstado();

    void setEstado(EstadoAsiento estado);

    /** Identificación legible: "B7". */
    String getCodigo();
}
