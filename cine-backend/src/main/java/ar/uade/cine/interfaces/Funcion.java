package ar.uade.cine.interfaces;

import java.time.LocalDateTime;

import ar.uade.cine.modelo.Idioma;
import ar.uade.cine.modelo.Proyeccion;

public interface Funcion {

    int getId();

    void setId(int id);

    int getPeliculaId();

    int getSalaId();

    LocalDateTime getInicio();

    Idioma getIdioma();

    Proyeccion getProyeccion();

    /** Precio base: lo que cuesta una butaca estándar. Los recargos se calculan aparte. */
    double getPrecio();
}
