package ar.uade.cine.dominio.salas;

/**
 * Sala de proyección. No guarda su distribución de butacas: las butacas son entidades
 * propias, y tener además la lista de cuántas hay por fila serían dos fuentes de verdad
 * para lo mismo. La distribución se usa una vez, al generarlas; después la sala se
 * describe por sus asientos.
 */
public interface Sala {

    int getId();

    void setId(int id);

    String getNombre();

    TipoSala getTipo();
}
