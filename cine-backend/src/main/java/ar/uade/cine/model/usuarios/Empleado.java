package ar.uade.cine.model.usuarios;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

import ar.uade.cine.model.usuarios.validacion.ValidadorEmpleado;

// Encargado o acomodador, entra con contraseña hasheada; sus datos propios los valida ValidadorEmpleado.
@Entity
@DiscriminatorValue("EMPLEADO")
@Getter
public class Empleado extends Usuario {

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    protected Empleado() {
    }

    public Empleado(String nombre, String email, String passwordHash, Rol rol) {
        super(nombre, email, rol);
        ValidadorEmpleado.rol(rol);
        this.passwordHash = ValidadorEmpleado.passwordHash(passwordHash);
    }

    public void reemplazarPasswordHash(String passwordHash) {
        this.passwordHash = ValidadorEmpleado.passwordHash(passwordHash);
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase(Locale.ROOT) + ")";
    }
}
