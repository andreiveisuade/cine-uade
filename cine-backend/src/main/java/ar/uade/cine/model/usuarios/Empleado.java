package ar.uade.cine.model.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Quien trabaja en el cine: el administrador, que carga películas, salas y programa
 * funciones, y el acomodador, que valida entradas en la puerta. A diferencia del cliente,
 * necesita iniciar sesión, y esa es exactamente la diferencia que justifica la herencia.
 *
 * <p>Antes se llamaba {@code AdministradorCine}, y el argumento era "solo el administrador
 * tiene credenciales". Al aparecer el acomodador esa frase dejó de ser cierta, pero el corte
 * real nunca fue el <em>cargo</em> sino <em>tener contraseña</em>: por eso se generalizó el
 * nombre en vez de sumar una clase hermana vacía. Cuál de los dos es lo dice {@link Rol}.
 */
@Entity
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

    /**
     * Contraseña ya hasheada. Nunca se guarda ni se compara en texto plano: el gestor
     * hashea lo que ingresa el usuario y compara los hashes.
     */
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

    /** Sin el hash: un toString no debería filtrar credenciales, ni siquiera hasheadas. */
    @Override
    public String toString() {
        return "[" + getId() + "] " + getNombre() + " <" + getEmail() + "> ("
                + getRol().name().toLowerCase() + ")";
    }
}
