package ar.uade.cine.service.usuarios;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.seguridad.LimiteDeIntentos;
import ar.uade.cine.infrastructure.seguridad.RegistroDeSesiones;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.DemasiadosIntentos;
import ar.uade.cine.model.rechazos.SinPrueba;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Contrasena;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.ventas.CodigoDeAcceso;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;

// La cuenta de un cliente ya abierta: datos, clave, recuperación y baja (R23 a R27); el alta es de GestorClientes.
// Las claves las compara y hashea solo el PasswordEncoder de seguridad; cada cambio de clave cierra sesiones.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorCuentas {

    private static final String CLAVE_ACTUAL_INCORRECTA = "La contraseña actual no es correcta";

    // R25: uno solo para email inexistente, sin cuenta, código ajeno o inventado: no delata qué emails existen.
    static final String SIN_CUENTA_PARA_ESE_CODIGO =
            "El email y el código no corresponden a ninguna cuenta: revisá el ticket de una de tus compras";

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder claves;
    private final LimiteDeIntentos intentos;
    private final RegistroDeSesiones sesiones;

    // R23
    public Cliente renombrar(String email, String nombre) {
        Cliente cliente = conCuenta(email);
        cliente.renombrar(nombre);
        return cliente;
    }

    // R24: la nueva se valida antes que la actual, como en el alta; la sesión que sigue es la del que la cambió.
    public void cambiarClave(String email, String actual, String nueva, String sesionQueSigue) {
        Cliente cliente = conCuenta(email);
        Contrasena clave = new Contrasena(nueva);
        exigirClave(cliente, actual);
        cliente.cambiarClave(claves.encode(clave.valor()));
        sesiones.cerrarLasDemas(cliente.getEmail(), sesionQueSigue);
    }

    // R25: el código de una de sus entradas prueba que es el dueño, como en R22; cuenta para el límite de intentos.
    public void recuperarClave(String email, String codigoReserva, String nueva, String ip) {
        String quien = Email.paraBuscar(email).map(Email::valor).orElse(email);
        if (intentos.bloqueado(quien, ip)) {
            throw new DemasiadosIntentos();
        }
        Contrasena clave = new Contrasena(nueva);
        Optional<Cliente> dueno = clienteRepository.findByEmail(quien)
                .filter(Cliente::tieneCuenta)
                .filter(cliente -> esDeSuReserva(codigoReserva, cliente));
        if (dueno.isEmpty()) {
            intentos.fallo(quien, ip);
            throw new SinPrueba(SIN_CUENTA_PARA_ESE_CODIGO);
        }
        intentos.exito(quien);
        Cliente cliente = dueno.get();
        cliente.cambiarClave(claves.encode(clave.valor()));
        sesiones.cerrarTodas(cliente.getEmail());
    }

    // R26: borra solo la clave; reservas, pagos y entradas son ventas del cine y quedan.
    public void darDeBaja(String email, String actual) {
        Cliente cliente = conCuenta(email);
        exigirClave(cliente, actual);
        cliente.cerrarCuenta();
        sesiones.cerrarTodas(cliente.getEmail());
    }

    // R27: se devuelve en claro una sola vez, para que el encargado se la pase al cliente; no se guarda ni se loguea.
    public Contrasena asignarClaveTemporal(int clienteId) {
        Cliente cliente = clienteRepository.exigir(clienteId, "el cliente");
        Contrasena temporal = Contrasena.temporal();
        cliente.asignarClaveTemporal(claves.encode(temporal.valor()));
        sesiones.cerrarTodas(cliente.getEmail());
        return temporal;
    }

    // La sesión es de un cliente con cuenta: la baja y los cambios de clave cierran las sesiones que la usaban.
    private Cliente conCuenta(String email) {
        return clienteRepository.findByEmail(email).filter(Cliente::tieneCuenta).orElseThrow();
    }

    private void exigirClave(Cliente cliente, String clave) {
        if (!claves.matches(clave, cliente.getPasswordHash())) {
            throw new DatoInvalido(CLAVE_ACTUAL_INCORRECTA);
        }
    }

    private boolean esDeSuReserva(String codigo, Cliente cliente) {
        return reservaRepository.existsByCodigoAndCliente_Id(new CodigoDeAcceso(codigo).valor(), cliente.getId());
    }
}
