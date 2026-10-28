package ar.uade.cine.model.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Quien compra entradas, por email; Usuario con rol fijo CLIENTE y sin clave: su cuenta es la de Google (R28).
// Reservar sin cuenta lo da de alta; entrar con Google después con ese email es el mismo cliente.
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

    protected Cliente() {
    }

    public Cliente(String nombre, String email) {
        super(nombre, email, Rol.CLIENTE);
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + ">";
    }
}
