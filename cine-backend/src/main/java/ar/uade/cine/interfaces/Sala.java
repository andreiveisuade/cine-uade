package ar.uade.cine.interfaces;

import java.util.List;

import ar.uade.cine.modelo.TipoSala;

public interface Sala {

    int getId();

    void setId(int id);

    String getNombre();

    TipoSala getTipo();

    /**
     * Butacas que tiene cada fila, de adelante hacia atrás: [8, 10, 12] significa
     * fila A con 8, fila B con 10 y fila C con 12. Las salas reales no son rectángulos.
     */
    List<Integer> getButacasPorFila();

    int getFilas();

    /** Suma de las butacas de todas las filas. */
    int getCapacidadSala();
}
