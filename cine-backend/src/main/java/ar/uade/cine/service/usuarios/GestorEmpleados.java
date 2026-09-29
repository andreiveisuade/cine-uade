package ar.uade.cine.service.usuarios;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Empleados para la sesión y el re-hash a bcrypt al entrar; el alta existe solo para los tests.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorEmpleados {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder claves;

    // Sin llamadas desde la API: no hay alta de administradores (el de demo lo siembra
    // seed/02-admin.sql). Queda para que los tests armen empleados con la clave ya en bcrypt.
    // La clave en claro solo la ve el gestor; nombre, email y rol los valida Empleado. Se busca
    // también entre los clientes: comparten el UNIQUE del email y EmpleadoRepository no los ve.
    public void registrar(String nombre, String email, String password, Rol rol) {
        if (password == null || password.length() < 6) {
            throw new DatoInvalido("La contraseña tiene que tener al menos 6 caracteres");
        }
        Empleado empleado = new Empleado(nombre, email, claves.encode(password), rol);
        if (empleadoRepository.existsByEmail(empleado.getEmail())
                || clienteRepository.existsByEmail(empleado.getEmail())) {
            throw new ConflictoDeNegocio("Ya existe un usuario con ese email");
        }
        empleadoRepository.save(empleado);
    }

    // El hash ya viene armado: lo pide el login, que es quien tiene la clave en claro.
    public void reemplazarHash(String email, String hashNuevo) {
        empleadoRepository.findByEmail(email).ifPresent(empleado -> empleado.reemplazarPasswordHash(hashNuevo));
    }

    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorEmail(String email) {
        return Email.paraBuscar(email).map(Email::valor).flatMap(empleadoRepository::findByEmail);
    }
}
