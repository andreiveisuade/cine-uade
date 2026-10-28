package ar.uade.cine.service.usuarios;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Contrasena;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Empleados para la sesión; el alta existe solo para los tests.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorEmpleados {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder claves;

    // Sin llamadas desde la API (el admin de demo sale de seed/02-admin.sql): queda para que los tests armen empleados.
    // Se busca también entre los clientes: comparten el UNIQUE del email y EmpleadoRepository no los ve.
    public void registrar(String nombre, String email, String password, Rol rol) {
        Contrasena clave = new Contrasena(password);
        Empleado empleado = new Empleado(nombre, email, claves.encode(clave.valor()), rol);
        if (empleadoRepository.existsByEmail(empleado.getEmail())
                || clienteRepository.existsByEmail(empleado.getEmail())) {
            throw new ConflictoDeNegocio("Ya existe un usuario con ese email");
        }
        empleadoRepository.save(empleado);
    }

    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorEmail(String email) {
        return Email.paraBuscar(email).flatMap(empleadoRepository::findByEmail);
    }
}
