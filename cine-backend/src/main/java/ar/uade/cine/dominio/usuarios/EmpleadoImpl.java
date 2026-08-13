package ar.uade.cine.dominio.usuarios;

public class EmpleadoImpl implements Empleado {

    private int id;
    private String nombre;
    private String email;
    private String passwordHash;
    private Rol rol;

    public EmpleadoImpl(String nombre, String email, String passwordHash, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public EmpleadoImpl(int id, String nombre, String email, String passwordHash, Rol rol) {
        this(nombre, email, passwordHash, rol);
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
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Dejó de estar fijo en ADMINISTRADOR: ahora un empleado puede ser también
     * ACOMODADOR, y el rol es lo que los distingue dentro de la misma clase.
     */
    @Override
    public Rol getRol() {
        return rol;
    }

    /** Sin el hash: un toString no debería filtrar credenciales, ni siquiera hasheadas. */
    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " <" + email + "> (" + rol.name().toLowerCase() + ")";
    }
}
