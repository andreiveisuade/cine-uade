package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import lombok.RequiredArgsConstructor;

import java.util.Map;

// Busca al cliente de una venta de candy por su email; aparte de la sesión, aunque los dos sean de usuarios.
@RequiredArgsConstructor
public final class ApiClientes {

    private final ClienteHttp http;

    public Cliente buscarClientePorEmail(String email) {
        return http.get("/clientes" + Parametros.consulta(Map.of("email", Parametros.oVacio(email))), Cliente.class);
    }
}
