package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.EmpleadoRepository;
import ar.uade.cine.service.ConflictoDeNegocio;

@Service
@Transactional
public class GestorEmpleados {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder claves;

    public GestorEmpleados(EmpleadoRepository empleadoRepository, PasswordEncoder claves) {
        this.empleadoRepository = empleadoRepository;
        this.claves = claves;
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
            throw new ConflictoDeNegocio("Ya hay un empleado con ese email");
        }
        empleadoRepository.save(new Empleado(nombre, email, claves.encode(password), rol));
    }

    // El hash ya viene armado: lo pide el login, que es quien tiene la clave en claro.
    public void reemplazarHash(String email, String hashNuevo) {
        empleadoRepository.findByEmail(email).ifPresent(empleado -> empleado.reemplazarPasswordHash(hashNuevo));
    }

    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorEmail(String email) {
        return empleadoRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Empleado> listar() {
        return empleadoRepository.findAll();
    }
}
