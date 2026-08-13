package ar.uade.cine.dominio.funciones;

import java.time.LocalDateTime;

public interface Funcion {

    int getId();

    void setId(int id);

    int getPeliculaId();

    int getSalaId();

    LocalDateTime getInicio();

    /** Doblada o subtitulada: es de esta proyección, no de la película. */
    Version getVersion();

    Proyeccion getProyeccion();

    /** Precio base: lo que cuesta una butaca estándar. Los recargos se calculan aparte. */
    double getPrecio();
}
