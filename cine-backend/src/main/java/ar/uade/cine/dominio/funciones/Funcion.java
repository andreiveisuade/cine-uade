package ar.uade.cine.dominio.funciones;

import java.time.LocalDateTime;

/**
 * Una función programada: una película en una sala, a una fecha y hora, con su versión,
 * formato y precio base. Referencia película y sala por id, no por objeto: quien
 * necesite los datos completos los pide al DAO correspondiente.
 */
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
