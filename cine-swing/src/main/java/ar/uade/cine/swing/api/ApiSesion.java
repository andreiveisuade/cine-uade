package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import lombok.RequiredArgsConstructor;

// El login del encargado o del acomodador; las credenciales las guarda ClienteHttp, que es quien las manda.
@RequiredArgsConstructor
public final class ApiSesion {

    private final ClienteHttp http;

    public Empleado login(String email, String password) {
        return http.ingresar("/sesion", email, password, Empleado.class);
    }
}
