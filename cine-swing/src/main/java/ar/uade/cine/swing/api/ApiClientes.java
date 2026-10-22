package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.ClaveTemporal;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

// Busca al cliente por su email (candy, boletería) y le asigna clave temporal; aparte de la sesión.
@RequiredArgsConstructor
public final class ApiClientes {

    private final ClienteHttp http;

    /** El backend contesta siempre una lista: con el cliente si el email existe, vacía si no. */
    public Optional<Cliente> buscarClientePorEmail(String email) {
        return http.lista("/clientes" + Parametros.consulta(Map.of("email", Parametros.oVacio(email))), Cliente.class)
                .stream().findFirst();
    }

    /** R27: la genera el backend; quien la recibe la muestra una vez y no la guarda. */
    public ClaveTemporal asignarClaveTemporal(int clienteId) {
        return http.post("/clientes/" + clienteId + "/clave-temporal", null, ClaveTemporal.class);
    }
}
