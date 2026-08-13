package ar.uade.cine.dominio.salas;

/**
 * El id llega vacío al construir de alta y con valor al reconstruir desde el DAO —
 * mismo patrón que el resto del dominio.
 */
public class SalaImpl implements Sala {

    private int id;
    private final String nombre;
    private final TipoSala tipo;

    public SalaImpl(String nombre, TipoSala tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public SalaImpl(int id, String nombre, TipoSala tipo) {
        this(nombre, tipo);
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public TipoSala getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo;
    }
}
