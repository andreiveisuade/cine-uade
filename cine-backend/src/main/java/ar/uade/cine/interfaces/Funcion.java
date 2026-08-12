package ar.uade.cine.interfaces;

import java.time.LocalDateTime;

public interface Funcion {

    int getId();

    void setId(int id);

    int getPeliculaId();

    int getSalaId();

    LocalDateTime getInicio();

    double getPrecio();
}
