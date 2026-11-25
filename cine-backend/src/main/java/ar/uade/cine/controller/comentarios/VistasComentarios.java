package ar.uade.cine.controller.comentarios;

import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.dto.comentarios.ComentarioVistaDTO;
import ar.uade.cine.model.comentarios.Comentario;

// Arma el JSON de un comentario; ensamblador.
@Component
public class VistasComentarios {

    public ComentarioVistaDTO comentario(Comentario comentario) {
        return new ComentarioVistaDTO(comentario.getId(), comentario.getCliente().getNombre(),
                comentario.getPuntaje(), comentario.getTexto(), Fechas.texto(comentario.getFecha()));
    }
}
