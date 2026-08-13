package ar.uade.cine.servicio;

import java.util.List;

import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.EmpleadoImpl;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.seguridad.Password;

/**
 * Alta e inicio de sesión de los empleados. El cliente no pasa por acá: compra
 * sin loguearse.
 */
public class GestorEmpleados {

    private final EmpleadoDAO dao;

    public GestorEmpleados(EmpleadoDAO dao) {
        this.dao = dao;
    }

    public void registrar(String nombre, String email, String password, Rol rol) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        // Un CLIENTE no tiene contraseña: darlo de alta acá lo convertiría en un usuario
        // que puede iniciar sesión, que es justo lo que el modelo dice que no existe.
        if (rol == null || !rol.esEmpleado()) {
            throw new IllegalArgumentException("El rol tiene que ser ADMINISTRADOR o ACOMODADOR");
        }
        if (dao.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un empleado con ese email");
        }
        dao.guardar(new EmpleadoImpl(nombre, email, Password.hashear(password), rol));
    }

    /**
     * Devuelve el empleado si las credenciales son correctas. El mensaje de error
     * es el mismo para email inexistente y contraseña equivocada: decir cuál de los dos
     * falló le confirma a un atacante qué emails están registrados.
     */
    public Empleado iniciarSesion(String email, String password) {
        return dao.buscarPorEmail(email)
                .filter(admin -> Password.coincide(password, admin.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));
    }

    public List<Empleado> listar() {
        return dao.listar();
    }
}
