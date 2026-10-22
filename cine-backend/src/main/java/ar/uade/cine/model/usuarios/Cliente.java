package ar.uade.cine.model.usuarios;

import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Quien compra entradas, por email; con cuenta si se registró con clave. Usuario con rol fijo CLIENTE.
// Reservar sin cuenta lo da de alta sin clave; registrarse después abre la cuenta de ese mismo cliente.
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

    // R27: la clave la eligió el encargado y la vio en pantalla; la web le pide cambiarla al entrar.
    @Getter
    @Column(name = "debe_cambiar_clave", nullable = false)
    private boolean debeCambiarClave;

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

    // R24 y R25: la elige el dueño, así que deja de ser temporal.
    public void cambiarClave(String passwordHash) {
        guardarPasswordHash(passwordHash);
        debeCambiarClave = false;
    }

    // R27: abre la cuenta si no la tenía, para quien perdió los tickets o nunca compró.
    public void asignarClaveTemporal(String passwordHash) {
        guardarPasswordHash(passwordHash);
        debeCambiarClave = true;
    }

    // R26: vuelve a ser cliente sin cuenta; sus reservas siguen siendo suyas y R22 la reabre con un código.
    public void cerrarCuenta() {
        guardarPasswordHash(null);
        debeCambiarClave = false;
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + ">";
    }
}
