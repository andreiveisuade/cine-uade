package ar.uade.cine.model.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Quien compra entradas. No tiene contraseña: reserva sin iniciar sesión, solo deja su
 * nombre y su email. Que la columna {@code password_hash} de la tabla admita NULL es
 * exactamente eso escrito en el schema.
 */
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
