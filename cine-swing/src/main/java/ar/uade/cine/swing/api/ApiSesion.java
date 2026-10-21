package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import lombok.RequiredArgsConstructor;

// El login del encargado o del acomodador; las credenciales las guarda ClienteHttp, que es quien las manda.
@RequiredArgsConstructor
public final class ApiSesion {

    private final ClienteHttp http;

    public Empleado login(String email, String password) {
        Empleado empleado = http.ingresar("/sesion", email, password, Empleado.class);
        if (!empleado.esEmpleado()) {
            // El backend ya aceptó la clave: se suelta y se rechaza como login fallido (401).
            http.olvidarCredenciales();
            throw new ErrorApi(401, "Esta aplicación es para empleados del cine: los clientes compran en la web");
        }
        return empleado;
    }
}
