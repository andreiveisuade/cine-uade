package ar.uade.cine.interfaces;

import java.util.List;

import ar.uade.cine.modelo.Genero;

public interface Pelicula {

    int getId();

    /** Lo usa el DAO para asignar el id que genera la base. */
    void setId(int id);

    String getTitulo();

    int getDuracionMinutos();

    List<Genero> getGeneros();

    void agregarGenero(Genero genero);
}
