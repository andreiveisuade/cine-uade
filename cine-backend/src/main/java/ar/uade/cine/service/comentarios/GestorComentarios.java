package ar.uade.cine.service.comentarios;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.comentarios.Comentario;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.comentarios.ComentarioRepository;
import ar.uade.cine.repository.usuarios.ClienteRepository;

// Publica y lista los comentarios del cine; el autor se busca por el email de la sesión y Comentario valida lo suyo.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorComentarios {

    private final ComentarioRepository comentarios;
    private final ClienteRepository clientes;
    private final Reloj reloj;

    public Comentario publicar(String email, Integer puntaje, String texto) {
        Cliente autor = clientes.findByEmail(email).orElseThrow();
        // Sin nanos: la columna es DATETIME y el alta contesta lo mismo que el listado.
        Comentario comentario = comentarios.save(new Comentario(autor, puntaje, texto, reloj.ahora().withNano(0)));
        log.info("comentario {} del cliente {}", comentario.getId(), autor.getId());
        return comentario;
    }

    @Transactional(readOnly = true)
    public List<Comentario> listar() {
        return comentarios.recientesPrimero();
    }
}
