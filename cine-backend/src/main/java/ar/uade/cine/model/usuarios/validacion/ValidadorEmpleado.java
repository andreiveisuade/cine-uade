package ar.uade.cine.model.usuarios.validacion;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.model.validacion.Regla;

// Valida lo propio de un empleado: un rol con login y la contraseña ya hasheada; Pure Fabrication.
public final class ValidadorEmpleado {

    private ValidadorEmpleado() {
    }

    // Un Empleado CLIENTE se guardaría con contraseña y, por la fórmula del discriminador en Usuario,
    // se releería como Cliente: la jerarquía dejaría de valer.
    public static void rol(Rol rol) {
        if (rol == null || !rol.esEmpleado()) {
            throw new DatoInvalido("El rol tiene que ser encargado o acomodador");
        }
    }

    // Sin hash no hay con qué comparar la clave al entrar: el empleado no podría iniciar sesión.
    public static String passwordHash(String passwordHash) {
        return Regla.texto(passwordHash).obligatorio("Falta la contraseña").valor();
    }
}
