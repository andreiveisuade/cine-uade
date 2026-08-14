package ar.uade.cine.dominio.salas;

/**
 * Sala de proyección. No guarda su distribución de butacas: las butacas son entidades
 * propias, y tener además la lista de cuántas hay por fila serían dos fuentes de verdad
 * para lo mismo. La distribución se usa una vez, al generarlas; después la sala se
 * describe por sus asientos.
 */
public interface Sala {

    /**
     * Lo que tarda la limpieza cuando nadie dijo otra cosa. Quince minutos es lo que lleva
     * levantar los pochoclos de una sala mediana entre dos funciones.
     */
    int LIMPIEZA_POR_DEFECTO = 15;

    int getId();

    void setId(int id);

    String getNombre();

    TipoSala getTipo();

    /**
     * Cuántos minutos hay que dejar entre el final de una función y el comienzo de la
     * siguiente para limpiar la sala.
     *
     * <p>Es un dato de la sala y no una constante del sistema porque no todas tardan lo
     * mismo: una de sesenta butacas se limpia mientras a la de doscientas todavía le
     * están saliendo. Que sea configurable es justamente lo que permite programar la sala
     * chica más apretada.
     *
     * <p>No es una entidad ni un bloque agendado, por el mismo test que dejó a la agenda
     * afuera del dominio: no guarda nada propio ni tiene ciclo de vida —se deriva entero
     * de la función anterior—. Es un margen, y por eso vive como un número acá.
     */
    int getMinutosLimpieza();
}
