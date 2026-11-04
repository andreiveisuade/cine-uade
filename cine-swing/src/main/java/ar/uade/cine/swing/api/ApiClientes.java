package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import ar.uade.cine.swing.api.dto.ventas.TransaccionesCliente;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// Los clientes: buscar por email (candy, boletería), listarlos y ver qué compró cada uno; aparte de la sesión.
@RequiredArgsConstructor
public final class ApiClientes {

    private final ClienteHttp http;

    public List<Cliente> obtenerClientes(Map<String, String> filtros) {
        return http.lista("/clientes" + Parametros.consulta(filtros), Cliente.class);
    }

    public TransaccionesCliente obtenerTransacciones(int clienteId) {
        return http.get("/clientes/" + clienteId + "/transacciones", TransaccionesCliente.class);
    }

    /** El backend contesta una lista, vacía si el email no existe: acá eso es el 404 que muestran las pantallas. */
    public Cliente buscarClientePorEmail(String email) {
        return http.lista("/clientes" + Parametros.consulta(Map.of("email", Parametros.oVacio(email))), Cliente.class)
                .stream().findFirst()
                .orElseThrow(() -> new ErrorApi(404, "No hay ningún cliente con el email " + email));
    }
}
