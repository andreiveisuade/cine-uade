package ar.uade.cine.servicio;

import java.util.List;

import ar.uade.cine.dominio.usuarios.AdministradorCine;
import ar.uade.cine.dominio.usuarios.AdministradorCineImpl;
import ar.uade.cine.persistencia.AdministradorDAO;

/**
 * Alta e inicio de sesión de los administradores. El cliente no pasa por acá: compra
 * sin loguearse.
 */
public class GestorAdministradores {

    private final AdministradorDAO dao;

    public GestorAdministradores(AdministradorDAO dao) {
        this.dao = dao;
    }

    public void registrar(String nombre, String email, String password) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        if (dao.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un administrador con ese email");
        }
        dao.guardar(new AdministradorCineImpl(nombre, email, Password.hashear(password)));
    }

    /**
     * Devuelve el administrador si las credenciales son correctas. El mensaje de error
     * es el mismo para email inexistente y contraseña equivocada: decir cuál de los dos
     * falló le confirma a un atacante qué emails están registrados.
     */
    public AdministradorCine iniciarSesion(String email, String password) {
        return dao.buscarPorEmail(email)
                .filter(admin -> Password.coincide(password, admin.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));
    }

    public List<AdministradorCine> listar() {
        return dao.listar();
    }
}
