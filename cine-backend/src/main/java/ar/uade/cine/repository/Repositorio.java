package ar.uade.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Base de los repositorios con id numérico; centraliza el «buscar por id o 404» (default, @NoRepositoryBean).
@NoRepositoryBean
public interface Repositorio<T> extends JpaRepository<T, Integer> {

    // `que` va con su artículo, como sale en el mensaje: "la película", "el producto".
    default T exigir(int id, String que) {
        return findById(id).orElseThrow(() -> new RecursoNoEncontrado("No existe " + que + " " + id));
    }
}
