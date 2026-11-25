package ar.uade.cine.repository.comentarios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import ar.uade.cine.model.comentarios.Comentario;
import ar.uade.cine.repository.Repositorio;

// Persistencia de comentarios; Repository de Spring Data: los valores van como parámetros, nunca en el SQL.
public interface ComentarioRepository extends Repositorio<Comentario> {

    @Query("select c from Comentario c join fetch c.cliente order by c.id desc")
    List<Comentario> recientesPrimero();
}
