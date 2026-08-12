package ar.uade.cine.interfaces;

import ar.uade.cine.modelo.Rol;

/**
 * Base de las personas que el sistema conoce. Los subtipos no se diferencian solo por
 * una etiqueta: el administrador tiene credenciales y el cliente no, porque el cliente
 * compra sin registrarse con contraseña.
 */
public interface Usuario {

    int getId();

    void setId(int id);

    String getNombre();

    String getEmail();

    /** Discriminador para la persistencia: en la base los dos van a la misma tabla. */
    Rol getRol();
}
