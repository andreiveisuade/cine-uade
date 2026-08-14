package ar.uade.cine.api;

import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dto.usuarios.PedidoClienteDTO;
import ar.uade.cine.servicio.GestorClientes;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Registro del cliente (CU-05). Es opcional: reservar también da de alta a quien compra
 * por primera vez. Son dos caminos al mismo alta, no un paso previo del otro.
 */
class RutasClientes {

    static void registrar(Javalin app, GestorClientes clientes, VistasUsuarios vistas) {

        app.get("/api/clientes", ctx -> {
            String email = ctx.queryParam("email");
            Optional<Cliente> cliente = email == null || email.isBlank()
                    ? Optional.empty()
                    : clientes.buscarPorEmail(email.trim());
            if (cliente.isEmpty()) {
                // Se usa para reconocer a alguien, no para verificar que exista: no
                // encontrarlo es una respuesta válida, no un error.
                ctx.contentType("application/json").result("null");
            } else {
                ctx.json(vistas.cliente(cliente.get()));
            }
        });

        app.post("/api/clientes", ctx -> {
            PedidoClienteDTO pedido = ctx.bodyAsClass(PedidoClienteDTO.class);
            Cliente cliente = clientes.registrar(pedido.nombre(), pedido.email());
            ctx.status(HttpStatus.CREATED).json(vistas.cliente(cliente));
        });
    }
}
