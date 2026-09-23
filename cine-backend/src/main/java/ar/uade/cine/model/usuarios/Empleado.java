package ar.uade.cine.model.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Quien trabaja en el cine (administrador o acomodador, según {@link Rol}). Lo que lo separa
 * del cliente, y justifica la herencia, es que inicia sesión.
 */
@Entity
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

    /** Nunca en texto plano: el gestor compara hashes. */
    @Column(name = "password_hash")
    private String passwordHash;

    protected Empleado() {
    }

    public Empleado(String nombre, String email, String passwordHash, Rol rol) {
        super(nombre, email, rol);
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /** Sin el hash: no filtrar credenciales, ni hasheadas. */
    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase() + ")";
    }
}
