package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.ClaveTemporal;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import lombok.RequiredArgsConstructor;

import java.util.Map;

// Busca al cliente por su email (candy, boletería) y le asigna clave temporal; aparte de la sesión.
@RequiredArgsConstructor
public final class ApiClientes {

    private final ClienteHttp http;

    /** El backend contesta una lista, vacía si el email no existe: acá eso es el 404 que muestran las pantallas. */
    public Cliente buscarClientePorEmail(String email) {
        return http.lista("/clientes" + Parametros.consulta(Map.of("email", Parametros.oVacio(email))), Cliente.class)
                .stream().findFirst()
                .orElseThrow(() -> new ErrorApi(404, "No hay ningún cliente con el email " + email));
    }

    /** R27: la genera el backend; quien la recibe la muestra una vez y no la guarda. */
    public ClaveTemporal asignarClaveTemporal(int clienteId) {
        return http.post("/clientes/" + clienteId + "/clave-temporal", null, ClaveTemporal.class);
    }
}
