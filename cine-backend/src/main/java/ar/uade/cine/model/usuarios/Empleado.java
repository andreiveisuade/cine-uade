package ar.uade.cine.model.usuarios;

import java.util.Locale;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import ar.uade.cine.model.usuarios.validacion.ValidadorEmpleado;

// Encargado o acomodador, entra con contraseña hasheada; sus datos propios los valida ValidadorEmpleado.
@Entity
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

    protected Empleado() {
    }

    public Empleado(String nombre, String email, String passwordHash, Rol rol) {
        super(nombre, email, rol);
        ValidadorEmpleado.rol(rol);
        guardarPasswordHash(ValidadorEmpleado.passwordHash(passwordHash));
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase(Locale.ROOT) + ")";
    }
}
