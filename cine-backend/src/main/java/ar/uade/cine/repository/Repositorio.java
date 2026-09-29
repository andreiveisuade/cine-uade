package ar.uade.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Base de los repositorios con id numérico; reúne en un solo lugar el «buscar por id o 404».
// Antes cada gestor y cada controller repetía `findById(id).orElseThrow(...)`, y había 8 métodos
// privados buscarOFallar/buscarFuncion que hacían lo mismo con su propio texto. Se recicla heredando
// una interfaz: el método default lo reciben todos los repositorios, y @NoRepositoryBean evita que
// Spring Data intente crear un bean para esta base genérica.
@NoRepositoryBean
public interface Repositorio<T> extends JpaRepository<T, Integer> {

    // `que` va con su artículo, como sale en el mensaje: "la película", "el producto".
    default T exigir(int id, String que) {
        return findById(id).orElseThrow(() -> new RecursoNoEncontrado("No existe " + que + " " + id));
    }
}
