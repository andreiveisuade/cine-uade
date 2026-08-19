package ar.uade.cine.model.salas;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Sala de proyección. No guarda su distribución de butacas: las butacas son entidades
 * propias, y tener además la lista de cuántas hay por fila serían dos fuentes de verdad
 * para lo mismo. La distribución se usa una vez, al generarlas; después la sala se
 * describe por sus asientos.
 *
 * <p>Era una interfaz con una única implementación, {@code SalaImpl}. Las dos se
 * fusionaron al pasar a JPA: la inversión de dependencias no la sostiene más un par
 * interfaz/clase por entidad —eso era una interfaz por las dudas, con un solo
 * implementador— sino {@link ar.uade.cine.repository.SalaRepositorio}, que es una
 * interfaz de verdad: el gestor depende de ella y la implementación la pone Spring Data.
 */
@Entity
public class Sala {

    /**
     * Lo que tarda la limpieza cuando nadie dijo otra cosa. Quince minutos es lo que lleva
     * levantar los pochoclos de una sala mediana entre dos funciones.
     */
    public static final int LIMPIEZA_POR_DEFECTO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoSala tipo;

    private int minutosLimpieza;

    /** Lo exige JPA para poder instanciar la entidad al leerla. Nadie más lo usa. */
    protected Sala() {
    }

    public Sala(String nombre, TipoSala tipo, int minutosLimpieza) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.minutosLimpieza = minutosLimpieza;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    /**
     * Cuántos minutos hay que dejar entre el final de una función y el comienzo de la
     * siguiente para limpiar la sala.
     *
     * <p>Es un dato de la sala y no una constante del sistema porque no todas tardan lo
     * mismo: una de sesenta butacas se limpia mientras a la de doscientas todavía le están
     * saliendo. Que sea configurable es justamente lo que permite programar la sala chica
     * más apretada.
     *
     * <p>No es una entidad ni un bloque agendado, por el mismo test que dejó a la agenda
     * afuera del dominio: no guarda nada propio ni tiene ciclo de vida —se deriva entero de
     * la función anterior—. Es un margen, y por eso vive como un número acá.
     */
    public int getMinutosLimpieza() {
        return minutosLimpieza;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo;
    }
}
