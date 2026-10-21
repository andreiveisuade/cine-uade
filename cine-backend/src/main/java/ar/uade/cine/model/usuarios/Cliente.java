package ar.uade.cine.model.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Quien compra entradas, por email; con cuenta si se registró con clave. Usuario con rol fijo CLIENTE.
// Reservar sin cuenta lo da de alta sin clave; registrarse después abre la cuenta de ese mismo cliente.
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

    protected Cliente() {
    }

    public Cliente(String nombre, String email) {
        super(nombre, email, Rol.CLIENTE);
    }

    public boolean tieneCuenta() {
        return getPasswordHash() != null;
    }

    // Una segunda vez pisaría la clave del dueño con la de quien conozca su email.
    public void abrirCuenta(String passwordHash) {
        if (tieneCuenta()) {
            throw new ConflictoDeNegocio("Ya existe una cuenta con ese email");
        }
        guardarPasswordHash(passwordHash);
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + ">";
    }
}
