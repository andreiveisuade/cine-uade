package ar.uade.cine.model.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

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
