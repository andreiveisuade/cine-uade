package ar.uade.cine.interfaces;

/**
 * Quien administra la cartelera: carga películas, salas y programa funciones.
 * A diferencia del cliente, necesita iniciar sesión.
 */
public interface AdministradorCine extends Usuario {

    /**
     * Contraseña ya hasheada. Nunca se guarda ni se compara en texto plano:
     * el gestor hashea lo que ingresa el usuario y compara los hashes.
     */
    String getPasswordHash();
}
