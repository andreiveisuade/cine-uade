package ar.uade.cine.dominio.usuarios;

/**
 * El id llega vacío al construir de alta y con valor al reconstruir desde el DAO —
 * mismo patrón que el resto del dominio.
 */
public class ClienteImpl implements Cliente {

    private int id;
    private String nombre;
    private String email;

    public ClienteImpl(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public ClienteImpl(int id, String nombre, String email) {
        this(nombre, email);
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
    public String getEmail() {
        return email;
    }

    @Override
    public Rol getRol() {
        return Rol.CLIENTE;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " <" + email + ">";
    }
}
