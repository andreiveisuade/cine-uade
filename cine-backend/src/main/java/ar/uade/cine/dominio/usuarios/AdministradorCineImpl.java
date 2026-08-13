package ar.uade.cine.dominio.usuarios;

public class AdministradorCineImpl implements AdministradorCine {

    private int id;
    private String nombre;
    private String email;
    private String passwordHash;

    public AdministradorCineImpl(String nombre, String email, String passwordHash) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public AdministradorCineImpl(int id, String nombre, String email, String passwordHash) {
        this(nombre, email, passwordHash);
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

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }

    /** Sin el hash: un toString no debería filtrar credenciales, ni siquiera hasheadas. */
    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " <" + email + "> (administrador)";
    }
}
