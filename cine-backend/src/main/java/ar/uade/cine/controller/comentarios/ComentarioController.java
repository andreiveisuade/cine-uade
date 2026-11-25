package ar.uade.cine.controller.comentarios;

import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.comentarios.ComentarioVistaDTO;
import ar.uade.cine.dto.comentarios.PedidoComentarioDTO;
import ar.uade.cine.service.comentarios.GestorComentarios;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/comentarios: traduce HTTP a GestorComentarios; el autor es siempre el de la sesión.
@Tag(name = "Comentarios", description = "Las reseñas de los clientes sobre el cine")
@RestController
@RequiredArgsConstructor
public class ComentarioController {

    private final GestorComentarios comentarios;
    private final VistasComentarios vistas;

    @Operation(summary = "Los comentarios, el más nuevo primero")
    @GetMapping("/api/comentarios")
    public List<ComentarioVistaDTO> listar() {
        return comentarios.listar().stream().map(vistas::comentario).toList();
    }

    // Alta sin Location: un comentario no tiene GET por id.
    @Operation(summary = "Publicar un comentario con puntaje de 1 a 5, a nombre del cliente de la sesión")
    @PostMapping("/api/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioVistaDTO publicar(@Valid @RequestBody PedidoComentarioDTO pedido, Principal cliente) {
        return vistas.comentario(comentarios.publicar(cliente.getName(), pedido.puntaje(), pedido.texto()));
    }
}
