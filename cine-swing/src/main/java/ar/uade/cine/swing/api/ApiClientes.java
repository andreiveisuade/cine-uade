package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

// Busca al cliente de una venta de candy por su email; aparte de la sesión, aunque los dos sean de usuarios.
@RequiredArgsConstructor
public final class ApiClientes {

    private final ClienteHttp http;

    /** El backend contesta siempre una lista: con el cliente si el email existe, vacía si no. */
    public Optional<Cliente> buscarClientePorEmail(String email) {
        return http.lista("/clientes" + Parametros.consulta(Map.of("email", Parametros.oVacio(email))), Cliente.class)
                .stream().findFirst();
    }
}
