package ar.uade.cine.model.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

// Encargado o acomodador, entra con contraseña hasheada; Experto: rechaza un rol que no sea de empleado.
@Entity
@DiscriminatorValue("EMPLEADO")
@Getter
public class Empleado extends Usuario {

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    protected Empleado() {
    }

    // Un Empleado CLIENTE se guardaría con contraseña y, por la fórmula del discriminador en
    // Usuario, se releería como Cliente: la jerarquía dejaría de valer. Por eso lo corta acá.
    public Empleado(String nombre, String email, String passwordHash, Rol rol) {
        super(nombre, email, rol);
        if (rol == null || !rol.esEmpleado()) {
            throw new IllegalArgumentException("El rol tiene que ser encargado o acomodador");
        }
        this.passwordHash = passwordHash;
    }

    public void reemplazarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase() + ")";
    }
}
