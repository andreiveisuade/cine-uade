package ar.uade.cine.model.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

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

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase() + ")";
    }
}
