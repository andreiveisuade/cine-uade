package ar.uade.cine.service.usuarios;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.EmpleadoRepository;
import ar.uade.cine.infrastructure.seguridad.Password;

/** Alta e inicio de sesión de empleados; el cliente compra sin loguearse. */
@Service
@Transactional
public class GestorEmpleados {

    private final EmpleadoRepository empleadoRepository;

    public GestorEmpleados(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
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
        // Un CLIENTE no tiene contraseña: darlo de alta acá le permitiría iniciar sesión.
        if (rol == null || !rol.esEmpleado()) {
            throw new IllegalArgumentException("El rol tiene que ser ADMINISTRADOR o ACOMODADOR");
        }
        if (empleadoRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un empleado con ese email");
        }
        empleadoRepository.save(new Empleado(nombre, email, Password.hashear(password), rol));
    }

    /** Mismo error para email inexistente y contraseña mala, para no revelar qué emails existen. */
    public Empleado iniciarSesion(String email, String password) {
        return empleadoRepository.findByEmail(email)
                .filter(admin -> Password.coincide(password, admin.getPasswordHash()))
                .orElseThrow(CredencialesInvalidas::new);
    }

    public List<Empleado> listar() {
        return empleadoRepository.findAll();
    }
}
